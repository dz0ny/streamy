VERSION := 0.0.9
PKG := streamy
COMMIT := $(shell git rev-parse HEAD)
BUILD_TIME := $(shell date -u +%FT%T)
BRANCH := $(shell git rev-parse --abbrev-ref HEAD)
CURRENT_TARGET = $(PKG)-$(shell uname -s)-$(shell uname -m)
TARGETS := Linux-arm-armv7l Linux-arm-armv6l Linux-arm64-aarch64 Linux-amd64-x86_64

os = $(word 1, $(subst -, ,$@))
arch = $(word 3, $(subst -, ,$@))
goarch = $(word 2, $(subst -, ,$@))
goos = $(shell echo $(os) | tr A-Z a-z)
output = $(PKG)-$(os)-$(arch)
version_flags = -X $(PKG)/version.Version=$(VERSION) \
 -X $(PKG)/version.CommitHash=${COMMIT} \
 -X $(PKG)/version.Branch=${BRANCH} \
 -X $(PKG)/version.BuildTime=${BUILD_TIME}

.PHONY: $(TARGETS)
$(TARGETS):
	env CGO_ENABLED=0 GOOS=$(goos) GOARCH=$(goarch) go build --ldflags '-s -w $(version_flags)' -o $(output) $(PKG)/cmd/$(PKG)

#
# Build all defined targets
#
.PHONY: build
build: $(TARGETS)

#
# Install app for current system
#
install: build
	sudo mv $(CURRENT_TARGET) /usr/local/bin/$(PKG)

#
# Install locked dependecies
#
sync: bin/dep
	cd src/$(PKG); dep init

#
# Update all locked dependecies
#
update: bin/dep
	cd src/$(PKG); dep ensure -update

bin/dep:
	go get -u github.com/golang/dep/cmd/dep

bin/github-release:
	go get github.com/aktau/github-release

bin/gocov:
	go get -u github.com/axw/gocov/gocov

bin/gometalinter:
	go get -u github.com/alecthomas/gometalinter
	bin/gometalinter --install --update

deps:
	go get -t $(PKG)/... # install test packages

clean:
	rm -f $(PKG)
	rm -rf pkg
	rm -rf bin
	find src/* -maxdepth 0 ! -name '$(PKG)' -type d | xargs rm -rf
	rm -rf src/$(PKG)/vendor/
	 
lint: bin/gometalinter
	bin/gometalinter --fast --disable=gotype --disable=gosimple --disable=ineffassign --disable=dupl --disable=gas --cyclo-over=30 --deadline=60s --exclude $(shell pwd)/src/$(PKG)/vendor src/$(PKG)/...
	find src/$(PKG) -not -path "./src/$(PKG)/vendor/*" -name '*.go' | xargs gofmt -w -s

test: deps lint cover
	go test -v -race $(shell go-ls $(PKG)/...)

cover: bin/gocov
	gocov test $(shell go-ls $(PKG)/...) | gocov report

all: deps sync build test

ui:
	cd web; npm run build
	rm -rf src/streamy/statik
	mv web/statik src/streamy
	
package: build
	mv -f $(PKG)-*-* service.streamy/bin/
	zip -r service.$(PKG)-${VERSION}-${BUILD_TIME}.zip service.$(PKG)

node_modules/.bin/api-console:
	npm install api-console-cli
	
docs: node_modules/.bin/api-console
	node_modules/.bin/api-console build api.raml

release: package
	github-release upload \
		--user dz0ny \
		--repo video.streamy \
		--tag "v$(VERSION)" \
		--name "service.$(PKG)-${VERSION}-${BUILD_TIME}.zip" \
		--file "service.$(PKG)-${VERSION}-${BUILD_TIME}.zip"

all: deps sync build test
