package core

import "expvar"

var (
	eventHandlerWebsocketReadClosed = expvar.NewInt("confluenceEventHandlerWebsocketReadClosed")
	eventHandlerContextDone         = expvar.NewInt("confluenceEventHandlerContextDone")
)
