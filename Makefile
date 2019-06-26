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
	cd streamy; env CGO_ENABLED=0 GOOS=$(goos) GOARCH=$(goarch) go build --gcflags "-trimpath $(shell pwd)" --ldflags '-s -w $(version_flags)' -o ../$(output) $(PKG)/cmd/$(PKG)

define localbuild
	GO111MODULE=off go get -u $(1)
	GO111MODULE=off go build $(1)
	mkdir -p bin
	mv $(2) bin/$(2)
endef

define ghupload
	bin/github-release upload \
		--user dz0ny \
		--repo $(PKG) \
		--tag "v$(VERSION)" \
		--name $(PKG)-$(1) \
		--file $(PKG)-$(1)
endef

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

bin/github-release:
	$(call localbuild,github.com/aktau/github-release,github-release)

bin/gocov:
	$(call localbuild,github.com/axw/gocov/gocov,gocov)

bin/golangci-lint:
	$(call localbuild,github.com/golangci/golangci-lint/cmd/golangci-lint,golangci-lint)

bin/statik:
	$(call localbuild,github.com/rakyll/statik,statik)

clean:
	rm -rf bin

lint: bin/golangci-lint
	bin/golangci-lint run
	go fmt

test: lint cover
	go test -v -race ./...

cover: bin/gocov
	gocov test ./... | gocov report

ui: bin/statik
	cd web; npm run build
	rm -rf streamy/statik
	mv web/statik streamy

package: build
	mv -f $(PKG)-*-* service.streamy/bin/
	zip -r service.$(PKG)-${VERSION}-${BUILD_TIME}.zip service.$(PKG)

tv:
	cd streamy; env GO111MODULE=off go get -u golang.org/x/mobile/cmd/gobind
	cd streamy; env GO111MODULE=off go get -u golang.org/x/mobile/cmd/gomobile
	cd streamy; env GO111MODULE=off go get -u golang.org/x/sys/unix
	cd streamy; env GO111MODULE=off gomobile init
	cd streamy; env GO111MODULE=on go mod vendor
	cd streamy; env GO111MODULE=off gomobile bind -target=android -v -ldflags '-s -w $(version_flags)' -o android/app/libs/tv.aar streamy/cmd/tv

release: package
	$(call ghupload,Linux-armv7l)
	$(call ghupload,Linux-armv6l)
	$(call ghupload,Linux-aarch64)
	$(call ghupload,Linux-x86_64)

all: deps sync build test
