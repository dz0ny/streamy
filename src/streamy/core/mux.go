package core

import (
	"log"
	"net/http"

	"github.com/go-chi/chi"
	"github.com/go-chi/docgen"
	"github.com/go-chi/docgen/raml"
	"github.com/go-chi/render"
	yaml "gopkg.in/yaml.v2"
)

var router = chi.NewRouter()

func init() {

	router.Get("/ping", func(w http.ResponseWriter, r *http.Request) {
		pong := struct {
			PING string
		}{"PONG"}
		render.JSON(w, r, pong)
	})

	// RESTy routes for "torrents" resource
	router.Route("/torrents", func(r chi.Router) {
		r.Use(render.SetContentType(render.ContentTypeJSON))
		r.Use(corsHandler)        // Inject CORS Headers
		r.Get("/", statusHandler) // GET /torrents
		r.Route("/add", func(r chi.Router) {
			r.Get("/", addMagnetHandler)
			r.Post("/", addTorrentHandler)
		})
		r.Route("/{torrentHash}", func(r chi.Router) {
			r.Use(torrentCtx)
			//r.Delete("/", deleteTorrent)     // DELETE /torrents/123
			r.Get("/", infoTorrent)          // GET /torrents/123
			r.Get("/events", eventHandler)   // GET /torrents/123/events
			r.Get("/info", fileStateHandler) // GET /torrents/123/info
			r.Get("/stream", dataHandler)    // GET /torrents/123/stream
		})
	})

	// Static router
	fileServerHandler(router, "/static", http.Dir("static"))
}

func APIdocs() string {
	ramlDocs := &raml.RAML{
		Title:     "Torrent API",
		Version:   "v1.0",
		MediaType: "application/json",
	}

	chi.Walk(router, func(method string, route string, handler http.Handler, middlewares ...func(http.Handler) http.Handler) error {
		handlerInfo := docgen.GetFuncInfo(handler)
		resource := &raml.Resource{}
		data := []byte(handlerInfo.Comment)
		if err := yaml.Unmarshal(data, resource); err != nil {
			log.Fatal(err)
		}
		return ramlDocs.Add(method, route, resource)
	})

	return ramlDocs.String()
}
