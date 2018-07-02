package tv

import (
	"log"
	"net"
	"net/http"
	"path"
	"time"

	"streamy/core"

	"github.com/anacrolix/missinggo/filecache"
	"github.com/anacrolix/missinggo/x"
	"github.com/anacrolix/tagflag"
	"github.com/anacrolix/torrent"

	"github.com/anacrolix/torrent/storage"
)

var flags = struct {
	Addr          string        `help:"HTTP listen address"`
	CacheCapacity tagflag.Bytes `help:"Data cache capacity"`
	TorrentGrace  time.Duration `help:"How long to wait to drop a torrent after its last request"`
	FileDir       string        `help:"Piece storage path"`
	Seed          bool          `help:"Seed data"`
	UploadRate    tagflag.Bytes `help:"Upload rate limit"`
	DownloadRate  tagflag.Bytes `help:"Download rate limit"`
	Prefetch      tagflag.Bytes `help:"Prefetch limit"`
	Debug         bool          `help:"Verbose output"`
}{
	Addr:          "0.0.0.0:9092",
	CacheCapacity: 2500 << 20,
	TorrentGrace:  time.Minute * 1,
	UploadRate:    25600,
	DownloadRate:  10485760,
	Prefetch:      100 << 20,
}

func newTorrentClient(dir string, freePort int) (ret *torrent.Client, err error) {

	storage := func() storage.ClientImpl {
		fc, err := filecache.NewCache(path.Join(dir, "cache"))
		x.Pie(err)
		fc.SetCapacity(flags.CacheCapacity.Int64())
		storageProvider := fc.AsResourceProvider()
		return storage.NewResourcePieces(storageProvider)
	}()
	conf := torrent.NewDefaultClientConfig()
	conf.DefaultStorage = storage
	conf.ListenPort = freePort

	conf.ExtendedHandshakeClientVersion = "Transmission/2.92"
	conf.HTTPUserAgent = "Transmission/2.92"
	conf.Bep20 = "-TR2920-"
	return torrent.NewClient(conf)
}

func getPort() int {
	addr, err := net.ResolveTCPAddr("tcp", "localhost:0")
	if err != nil {
		panic(err)
	}

	l, err := net.ListenTCP("tcp", addr)
	if err != nil {
		panic(err)
	}
	defer l.Close()
	return l.Addr().(*net.TCPAddr).Port
}

var TCPServer net.Listener
var TClient *torrent.Client

func Start(dir string) {
	go func() {
		log.SetFlags(log.Flags() | log.Lshortfile)

		freePort := getPort()

		log.Printf("Torrent client port: %d", freePort)
		core.StorageRoot = dir
		TClient, err := newTorrentClient(dir, freePort)
		if err != nil {
			log.Fatalf("error creating torrent client: %s", err)
		}

		TCPServer, err = net.Listen("tcp4", flags.Addr)
		if err != nil {
			log.Fatal(err)
		}

		log.Printf("serving http at %s", TCPServer.Addr())

		h := &core.Handler{
			TC:                TClient,
			TorrentCloseGrace: flags.TorrentGrace,
		}
		err = http.Serve(TCPServer, h)
		if err != nil {
			log.Fatal(err)
		}
	}()
}

func Stop() {
	TClient.Close()
	TCPServer.Close()
}
