package core

import (
	"context"
	"net/http"

	"github.com/anacrolix/torrent/metainfo"

	"github.com/go-chi/chi"
)

func torrentCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		qih := chi.URLParam(r, "torrentHash")

		if qih == "" {
			http.Error(w, "Torrent not found", http.StatusNotFound)
		}
		var ih metainfo.Hash
		if err := ih.FromHexString(qih); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		t := getTorrentHandle(r, ih)
		ctx := context.WithValue(r.Context(), torrentContextKey, t)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
