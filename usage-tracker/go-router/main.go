package main

import (
	"net/http"

	"fmt"
	"sync"
	"time"

	"github.com/go-chi/chi/v5/middleware"
)

type LimitTracker struct {
	mu   sync.Mutex
	reqs []time.Time
}
type Results struct {
	Start         time.Time `json:"start"`
	End           time.Time `json:"end"`
	TotalSeconds  float64   `json:"totalSeconds"`
	RpmPerTenSec  []float64 `json:"rpmPerTenSec"`
	Rpm           []float64 `json:"rpm"`
	TotalRequests int       `json:"totalRequests"`
	RpmPerSec     []float64 `json:"rpmPerSec"`
}

type Middleware func(http.Handler) http.Handler

// for custom middleware, it should really be like this: https://youtu.be/H7tbjKFSg58?si=_P1YSjae4sIksT6v&t=571
func myCustomMiddleware(next http.Handler) http.Handler {
	fmt.Println("hi there")
	return next
}

func chainMiddleware(next http.Handler, middle ...Middleware) http.Handler {
	for _, m := range middle {
		next = m(next)
	}

	return next
}

var l = LimitTracker{}

func main() {
	router := http.NewServeMux()

	// record the request
	router.HandleFunc("GET /", root)
	router.HandleFunc("DELETE /", del)
	// collate the results
	router.HandleFunc("GET /results", results)

	// create server with middleware
	port := 3000
	handler := chainMiddleware(router, middleware.Logger, myCustomMiddleware)
	s := http.Server{Addr: fmt.Sprintf(":%d", port), Handler: handler}
	fmt.Printf("Server running on port %d\n", port)

	s.ListenAndServe()
}
