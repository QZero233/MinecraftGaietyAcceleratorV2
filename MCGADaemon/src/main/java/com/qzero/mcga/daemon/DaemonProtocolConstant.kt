package com.qzero.mcga.daemon

class DaemonProtocolConstant {

    companion object {
        const val TYPE_NEW_PROCESS = "NP"
        const val TYPE_WRITE_STD_IO = "WS"
        const val TYPE_RECEIVED_STD_IO = "RS"
        const val TYPE_REPORT_PROCESS_STATE = "PS"

        const val PROCESS_STATE_RUNNING = "1"
        const val PROCESS_STATE_IDLE = "0"
    }

}