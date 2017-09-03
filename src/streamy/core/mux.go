package core

import (
	"net/http"

	"github.com/gorilla/mux"
	"github.com/justinas/alice"
)

var router = mux.NewRouter()

func init() {
	tc := alice.New(withTorrentContext)
	router.HandleFunc("/ping", pingHandler)
	router.HandleFunc("/torrents", statusHandler)
	router.HandleFunc("/torrent/add", addHandler)
	router.Handle("/torrent/{ih}", tc.ThenFunc(infoHandler)).Name("info")
	router.Handle("/torrent/{ih}/html", tc.ThenFunc(htmlHandler)).Name("html")
	router.Handle("/torrent/{ih}/play", tc.ThenFunc(playHandler)).Name("play")
	router.Handle("/torrent/{ih}/events", tc.ThenFunc(eventHandler)).Name("events")
	router.Handle("/torrent/{ih}/info", tc.ThenFunc(fileStateHandler)).Name("file")
	router.Handle("/torrent/{ih}/stream", tc.ThenFunc(dataHandler)).Name("stream")

	// Static router
	s := http.StripPrefix("/web/", http.FileServer(http.Dir("./web/")))
	router.PathPrefix("/web/").Handler(s)
}
