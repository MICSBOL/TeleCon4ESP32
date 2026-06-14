package com.micsbol.telecon4esp32.domain.bluetooth

import java.io.IOException

class TransferFailedException: IOException("Reading incoming data failed")