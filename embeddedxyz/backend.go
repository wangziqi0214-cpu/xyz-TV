package embeddedxyz

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

const (
	appBaseURL       = "https://api.xiaoyuzhoufm.com"
	podcasterBaseURL = "https://podcaster-api.xiaoyuzhoufm.com"
)

var (
	serverMu   sync.Mutex
	serverInst *http.Server
	boundPort  = 23020
	httpClient = &http.Client{Timeout: 15 * time.Second}
)

type sendCodeRequest struct {
	MobilePhoneNumber string `json:"mobilePhoneNumber"`
	AreaCode          string `json:"areaCode"`
}

type loginRequest struct {
	AreaCode          string `json:"areaCode"`
	VerifyCode        string `json:"verifyCode"`
	MobilePhoneNumber string `json:"mobilePhoneNumber"`
}

type searchRequest struct {
	Pid         string          `json:"pid"`
	Type        string          `json:"type"`
	Keyword     string          `json:"keyword"`
	LoadMoreKey json.RawMessage `json:"loadMoreKey"`
}

type discoveryRequest struct {
	LoadMoreKey string `json:"loadMoreKey"`
}

type podcastDetailRequest struct {
	Pid string `json:"pid"`
}

type episodeListRequest struct {
	Pid         string          `json:"pid"`
	Order       string          `json:"order"`
	LoadMoreKey json.RawMessage `json:"loadMoreKey"`
}

type episodeDetailRequest struct {
	Eid string `json:"eid"`
}

func Start() string {
	return StartOnPort(boundPort)
}

func StartOnPort(port int) string {
	serverMu.Lock()
	defer serverMu.Unlock()

	if serverInst != nil {
		boundPort = port
		return ""
	}

	gin.SetMode(gin.ReleaseMode)
	engine := gin.New()
	engine.Use(gin.Recovery(), cors())

	engine.GET("/ping", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"code":    http.StatusOK,
			"message": "pong",
			"version": "embedded",
		})
	})

	engine.POST("/sendCode", handleSendCode)
	engine.POST("/login", handleLogin)
	engine.POST("/search", requireToken(), handleSearch)
	engine.POST("/discovery", requireToken(), handleDiscovery)
	engine.POST("/podcast_detail", requireToken(), handlePodcastDetail)
	engine.POST("/episode_list", requireToken(), handleEpisodeList)
	engine.POST("/episode_detail", requireToken(), handleEpisodeDetail)

	addr := fmt.Sprintf("127.0.0.1:%d", port)
	srv := &http.Server{
		Addr:              addr,
		Handler:           engine,
		ReadHeaderTimeout: 15 * time.Second,
	}

	serverInst = srv
	boundPort = port

	go func() {
		_ = srv.ListenAndServe()
		serverMu.Lock()
		defer serverMu.Unlock()
		if serverInst == srv {
			serverInst = nil
		}
	}()

	return ""
}

func Stop() string {
	serverMu.Lock()
	srv := serverInst
	serverInst = nil
	serverMu.Unlock()

	if srv == nil {
		return ""
	}

	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		return err.Error()
	}

	return ""
}

func BaseURL() string {
	return fmt.Sprintf("http://127.0.0.1:%d/", boundPort)
}

func handleSendCode(c *gin.Context) {
	var req sendCodeRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.MobilePhoneNumber == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}
	if req.AreaCode == "" {
		req.AreaCode = "+86"
	}

	status, payload, err := doJSONRequest(
		http.MethodPost,
		podcasterBaseURL+"/v1/auth/send-code",
		req,
		authHeaders(),
	)
	if err != nil {
		c.JSON(status, gin.H{"code": status, "msg": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code": http.StatusOK,
		"msg":  "OK",
		"data": payload,
	})
}

func handleLogin(c *gin.Context) {
	var req loginRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.MobilePhoneNumber == "" || req.VerifyCode == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}
	if req.AreaCode == "" {
		req.AreaCode = "+86"
	}

	status, payload, headers, err := doJSONRequestWithHeaders(
		http.MethodPost,
		podcasterBaseURL+"/v1/auth/login-with-sms",
		req,
		authHeaders(),
	)
	if err != nil {
		c.JSON(status, gin.H{"code": status, "msg": err.Error(), "data": payload})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code": http.StatusOK,
		"msg":  "OK",
		"data": gin.H{
			"data":                 payload,
			"x-jike-access-token":  headers.Get("x-jike-access-token"),
			"x-jike-refresh-token": headers.Get("x-jike-refresh-token"),
		},
	})
}

func handleSearch(c *gin.Context) {
	var req searchRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.Keyword == "" || req.Type == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}

	body := map[string]any{
		"limit":           "20",
		"sourcePageName":  "4",
		"type":            req.Type,
		"currentPageName": "4",
		"keyword":         req.Keyword,
	}
	if req.Pid != "" {
		body["pid"] = req.Pid
	}
	if len(req.LoadMoreKey) > 0 && string(req.LoadMoreKey) != "null" {
		var value any
		if json.Unmarshal(req.LoadMoreKey, &value) == nil {
			body["loadMoreKey"] = value
		}
	}

	proxyJSON(c, http.MethodPost, appBaseURL+"/v1/search/create", body, contentHeaders(tokenFrom(c)))
}

func handleDiscovery(c *gin.Context) {
	var req discoveryRequest
	_ = c.ShouldBindJSON(&req)

	body := map[string]any{
		"returnAll": "false",
	}
	if req.LoadMoreKey != "" {
		body["loadMoreKey"] = req.LoadMoreKey
	}

	proxyJSON(c, http.MethodPost, appBaseURL+"/v1/discovery-feed/list", body, contentHeaders(tokenFrom(c)))
}

func handlePodcastDetail(c *gin.Context) {
	var req podcastDetailRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.Pid == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}

	endpoint := appBaseURL + "/v1/podcast/get?pid=" + url.QueryEscape(req.Pid)
	proxyJSON(c, http.MethodGet, endpoint, nil, contentHeaders(tokenFrom(c)))
}

func handleEpisodeList(c *gin.Context) {
	var req episodeListRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.Pid == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}

	order := req.Order
	if order != "asc" && order != "desc" {
		order = "desc"
	}

	body := map[string]any{
		"limit": "20",
		"pid":   req.Pid,
		"order": order,
	}
	if len(req.LoadMoreKey) > 0 && string(req.LoadMoreKey) != "null" {
		var value any
		if json.Unmarshal(req.LoadMoreKey, &value) == nil {
			body["loadMoreKey"] = value
		}
	}

	proxyJSON(c, http.MethodPost, appBaseURL+"/v1/episode/list", body, contentHeaders(tokenFrom(c)))
}

func handleEpisodeDetail(c *gin.Context) {
	var req episodeDetailRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.Eid == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "invalid request"})
		return
	}

	endpoint := appBaseURL + "/v1/episode/get?eid=" + url.QueryEscape(req.Eid)
	proxyJSON(c, http.MethodGet, endpoint, nil, contentHeaders(tokenFrom(c)))
}

func proxyJSON(c *gin.Context, method, endpoint string, body any, headers map[string]string) {
	status, payload, err := doJSONRequest(method, endpoint, body, headers)
	if err != nil {
		c.JSON(status, gin.H{"code": status, "msg": err.Error(), "data": payload})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code": http.StatusOK,
		"msg":  "OK",
		"data": payload,
	})
}

func doJSONRequest(method, endpoint string, body any, headers map[string]string) (int, any, error) {
	status, payload, _, err := doJSONRequestWithHeaders(method, endpoint, body, headers)
	return status, payload, err
}

func doJSONRequestWithHeaders(method, endpoint string, body any, headers map[string]string) (int, any, http.Header, error) {
	var reader io.Reader
	if body != nil {
		raw, err := json.Marshal(body)
		if err != nil {
			return http.StatusInternalServerError, nil, nil, err
		}
		reader = bytes.NewReader(raw)
	}

	req, err := http.NewRequest(method, endpoint, reader)
	if err != nil {
		return http.StatusInternalServerError, nil, nil, err
	}

	for key, value := range headers {
		req.Header.Set(key, value)
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}

	resp, err := httpClient.Do(req)
	if err != nil {
		return http.StatusBadGateway, nil, nil, err
	}
	defer resp.Body.Close()

	rawBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return http.StatusBadGateway, nil, resp.Header, err
	}

	payload, parseErr := decodeJSON(rawBody)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		if parseErr != nil {
			return resp.StatusCode, string(rawBody), resp.Header, fmt.Errorf("upstream status %d", resp.StatusCode)
		}
		return resp.StatusCode, payload, resp.Header, fmt.Errorf("upstream status %d", resp.StatusCode)
	}
	if parseErr != nil {
		return http.StatusBadGateway, string(rawBody), resp.Header, fmt.Errorf("failed to decode upstream JSON: %w", parseErr)
	}

	return resp.StatusCode, payload, resp.Header, nil
}

func decodeJSON(raw []byte) (any, error) {
	if len(bytes.TrimSpace(raw)) == 0 {
		return map[string]any{}, nil
	}

	var value any
	if err := json.Unmarshal(raw, &value); err != nil {
		return nil, err
	}
	return value, nil
}

func requireToken() gin.HandlerFunc {
	return func(c *gin.Context) {
		if tokenFrom(c) == "" {
			c.JSON(http.StatusBadRequest, gin.H{"code": http.StatusBadRequest, "msg": "missing access token"})
			c.Abort()
			return
		}
		c.Next()
	}
}

func tokenFrom(c *gin.Context) string {
	return c.GetHeader("x-jike-access-token")
}

func authHeaders() map[string]string {
	return map[string]string{
		"Accept":          "application/json, text/plain, */*",
		"Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
		"Origin":          "https://podcaster.xiaoyuzhoufm.com",
		"Referer":         "https://podcaster.xiaoyuzhoufm.com/",
		"User-Agent":      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36",
	}
}

func contentHeaders(token string) map[string]string {
	now := time.Now().Format(time.RFC3339)
	return map[string]string{
		"Accept":                      "*/*",
		"User-Agent":                  "Xiaoyuzhou/2.57.1 (build:1576; iOS 17.4.1)",
		"Market":                      "AppStore",
		"App-BuildNo":                 "1576",
		"OS":                          "ios",
		"x-jike-access-token":         token,
		"Manufacturer":                "Apple",
		"BundleID":                    "app.podcast.cosmos",
		"Connection":                  "keep-alive",
		"abtest-info":                 "{\"old_user_discovery_feed\":\"enable\"}",
		"Accept-Language":             "zh-Hant-HK;q=1.0, zh-Hans-CN;q=0.9",
		"Model":                       "iPhone14,2",
		"app-permissions":             "4",
		"App-Version":                 "2.57.1",
		"WifiConnected":               "true",
		"OS-Version":                  "17.4.1",
		"x-custom-xiaoyuzhou-app-dev": "",
		"Local-Time":                  now,
		"Timezone":                    "Asia/Shanghai",
	}
}

func cors() gin.HandlerFunc {
	return func(c *gin.Context) {
		origin := c.GetHeader("Origin")
		if origin == "" {
			origin = "*"
		}
		c.Header("Access-Control-Allow-Origin", origin)
		c.Header("Vary", "Origin")
		c.Header("Access-Control-Allow-Headers", "Content-Type, AccessToken, X-CSRF-Token, Authorization, Token, x-token, x-jike-access-token, x-jike-refresh-token")
		c.Header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH, PUT")
		c.Header("Access-Control-Expose-Headers", "Content-Length, Access-Control-Allow-Origin, Access-Control-Allow-Headers, Content-Type")
		if origin != "*" {
			c.Header("Access-Control-Allow-Credentials", "true")
		}

		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}

		c.Next()
	}
}
