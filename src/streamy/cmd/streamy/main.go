package main

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"time"

	"github.com/anacrolix/dht"
	"github.com/anacrolix/missinggo/filecache"
	"github.com/anacrolix/missinggo/x"
	"github.com/anacrolix/tagflag"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/iplist"
	"github.com/anacrolix/torrent/storage"
	"github.com/jackpal/gateway"

	"streamy/core"
)

var version = ""
var commitHash = ""
var branch = ""
var buildTime = ""

var flags = struct {
	Addr          string        `help:"HTTP listen address"`
	CacheCapacity tagflag.Bytes `help:"Data cache capacity"`
	TorrentGrace  time.Duration `help:"How long to wait to drop a torrent after its last request"`
	FileDir       string        `help:"File-based storage directory, overrides piece storage"`
	Seed          bool          `help:"Seed data"`
	Debug         bool          `help:"Verbose output"`
}{
	Addr:          "0.0.0.0:9092",
	CacheCapacity: 10 << 29,
	TorrentGrace:  time.Minute * 2,
}

func newTorrentClient(freePort int, ext net.IP) (ret *torrent.Client, err error) {

	if err != nil {
		panic(err)
	}
	blocklist, err := iplist.MMapPacked("packed-blocklist")
	if err != nil {
		log.Print(err)
	}
	storage := func() storage.ClientImpl {
		if flags.FileDir != "" {
			return storage.NewFile(flags.FileDir)
		}
		fc, err := filecache.NewCache("filecache")
		x.Pie(err)
		fc.SetCapacity(flags.CacheCapacity.Int64())
		storageProvider := fc.AsResourceProvider()
		return storage.NewResourcePieces(storageProvider)
	}()
	return torrent.NewClient(&torrent.Config{
		IPBlocklist:    blocklist,
		DefaultStorage: storage,
		DHTConfig: dht.ServerConfig{
			PublicIP:      ext,
			StartingNodes: dht.GlobalBootstrapAddrs,
		},
		Seed:        flags.Seed,
		Debug:       flags.Debug,
		DisableIPv6: true,
		ListenAddr:  fmt.Sprintf(":%d", freePort),
	})
}

func GetPort() int {
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
	tagflag.Description(fmt.Sprintf("Streamy %s built at %s from commit %s@%s", version, buildTime, commitHash, branch))
	tagflag.Parse(&flags)

	freePort := GetPort()

	var gatewayIP net.IP
	var ext net.IP
	log.Printf("Torrent client port: %d", freePort)
	log.Printf("useNATPMP but gateway not provided, trying discovery")
	gatewayIP, err := gateway.DiscoverGateway()
	if err != nil {
		return
	}
	log.Printf("...discovered gateway IP: %s", gatewayIP)
	log.Println("Using NAT-PMP to open port.")
	if gatewayIP != nil {
		nat := core.NewNatPMP(gatewayIP)
		nat.AddPortMapping("tcp", freePort, freePort, "Streamy port TCP", 360000)
		nat.AddPortMapping("udp", freePort, freePort, "Streamy port TCP", 360000)
		defer nat.DeletePortMapping("tcp", freePort, freePort)
		defer nat.DeletePortMapping("udp", freePort, freePort)
		ext, err = nat.GetExternalAddress()
		log.Printf("...discovered external IP: %s", ext)
	}

	cl, err := newTorrentClient(freePort, ext)
	if err != nil {
		log.Fatalf("error creating torrent client: %s", err)
	}
	defer cl.Close()

	l, err := net.Listen("tcp4", flags.Addr)
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()
	log.Println(core.APIdocs())
	log.Printf("serving http at %s", l.Addr())
	h := &core.Handler{cl, flags.TorrentGrace}

	err = http.Serve(l, h)
	if err != nil {
		log.Fatal(err)
	}
}
