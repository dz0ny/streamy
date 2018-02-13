package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"time"

	"streamy/core"
	"streamy/fs"
	"streamy/version"

	"github.com/anacrolix/dht"
	"github.com/anacrolix/missinggo/filecache"
	"github.com/anacrolix/missinggo/x"
	"github.com/anacrolix/tagflag"
	"github.com/anacrolix/torrent"
	"github.com/hashicorp/mdns"
	"github.com/jacobsa/fuse"

	"github.com/anacrolix/torrent/iplist"
	"github.com/anacrolix/torrent/storage"
	"golang.org/x/time/rate"
)

var flags = struct {
	Addr                  string        `help:"HTTP listen address"`
	CacheCapacity         tagflag.Bytes `help:"Data cache capacity"`
	TorrentGrace          time.Duration `help:"How long to wait to drop a torrent after its last request"`
	FileDir               string        `help:"Piece storage path"`
	Seed                  bool          `help:"Seed data"`
	UploadRate            tagflag.Bytes `help:"Upload rate limit"`
	DownloadRate          tagflag.Bytes `help:"Download rate limit"`
	Prefetch              tagflag.Bytes `help:"Prefetch limit"`
	Debug                 bool          `help:"Verbose output"`
	ConnectionsPerTorrent int           `help:"Limit Connections per torrent"`
	MountDir              string        `help:"location where torrent contents are made available"`
}{
	Addr:                  "0.0.0.0:9092",
	CacheCapacity:         4000 << 20,
	TorrentGrace:          time.Minute * 1,
	FileDir:               "filecache",
	UploadRate:            25600,
	DownloadRate:          1048576,
	ConnectionsPerTorrent: 40,
	Prefetch:              100 << 20,
	MountDir:              "",
}

func newTorrentClient(freePort int) (ret *torrent.Client, err error) {

	if err != nil {
		panic(err)
	}
	blocklist, err := iplist.MMapPackedFile("packed-blocklist")
	if err != nil {
		log.Print(err)
	} else {
		defer func() {
			if err != nil {
				blocklist.Close()
			} else {
				go func() {
					<-ret.Closed()
					blocklist.Close()
				}()
			}
		}()
	}
	storage := func() storage.ClientImpl {
		fc, err := filecache.NewCache(flags.FileDir)
		x.Pie(err)
		fc.SetCapacity(flags.CacheCapacity.Int64())
		storageProvider := fc.AsResourceProvider()
		return storage.NewResourcePieces(storageProvider)
	}()

	return torrent.NewClient(&torrent.Config{
		DHTConfig: dht.ServerConfig{
			StartingNodes: dht.GlobalBootstrapAddrs,
		},
		DefaultStorage: storage,
		IPBlocklist:    blocklist,
		Seed:           flags.Seed,
		Debug:          flags.Debug,

		UploadRateLimiter:   rate.NewLimiter(rate.Limit(flags.UploadRate), 256<<10),
		DownloadRateLimiter: rate.NewLimiter(rate.Limit(flags.DownloadRate), 1<<20),

		EstablishedConnsPerTorrent: flags.ConnectionsPerTorrent,

		ExtendedHandshakeClientVersion: "Transmission/2.92",
		HTTPUserAgent:                  "Transmission/2.92",
		Bep20:                          "-TR2920-",

		ListenAddr: fmt.Sprintf(":%d", freePort),
	})
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

func main() {
	log.SetFlags(log.Flags() | log.Lshortfile)
	desc := tagflag.Description(fmt.Sprintf(
		"Streamy %s built at %s from commit %s@%s",
		version.Version, version.BuildTime, version.CommitHash, version.Branch,
	))
	tagflag.Parse(&flags, desc)

	freePort := getPort()

	log.Printf("Torrent client port: %d", freePort)

	cl, err := newTorrentClient(freePort)
	if err != nil {
		log.Fatalf("error creating torrent client: %s", err)
	}
	defer cl.Close()

	if flags.MountDir != "" {
		go func() {
			cfg := &fuse.MountConfig{
				ReadOnly: true,
			}
			fsServer, err := fs.NewtorrentFS(cl)
			if err != nil {
				log.Fatalf("NewtorrentFS %v", err)
			}
			log.Printf("NewtorrentFS: %v", fsServer)
			mfs, err := fuse.Mount(flags.MountDir, fsServer, cfg)
			if err != nil {
				log.Fatalf("Mount: %v", err)
			}
			// Wait for it to be unmounted.
			if err = mfs.Join(context.Background()); err != nil {
				log.Fatalf("Join: %v", err)
			}
		}()

	}

	l, err := net.Listen("tcp4", flags.Addr)
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()

	log.Printf("serving http at %s", l.Addr())

	host, _ := os.Hostname()
	info := []string{"My awesome service"}
	service, _ := mdns.NewMDNSService(host, "_foobar._tcp", "", "", 8000, nil, info)

	// Create the mDNS server, defer shutdown
	server, _ := mdns.NewServer(&mdns.Config{Zone: service})
	defer server.Shutdown()

	h := &core.Handler{
		TC:                cl,
		TorrentCloseGrace: flags.TorrentGrace,
	}
	err = http.Serve(l, h)
	if err != nil {
		log.Fatal(err)
	}
}
