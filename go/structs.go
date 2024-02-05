package main

import "time"

type Req struct {
	url    string
	params map[string]int
}
type Results struct {
	Start         time.Time `json:"start"`
	End           time.Time `json:"end"`
	TotalSeconds  float64   `json:"totalSeconds"`
	RpmPerTenSec  []float64 `json:"rpmPerTenSec"`
	Rpm           []float64 `json:"rpm"`
	TotalRequests int       `json:"totalRequests"`
	// RpmPerSec     []float64 `json:"rpmPerSec"`
	RpmPerSec []float64 `json:"-"`
}
