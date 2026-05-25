package com.example.utils

import java.util.Locale

object TrafficUtils {
    fun formatSpeed(bytesPerSec: Long, useBits: Boolean = false): String {
        return if (!useBits) {
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB/s", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB/s", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB/s", kb)
                else -> "$bytesPerSec B/s"
            }
        } else {
            val bitsPerSec = bytesPerSec * 8.0
            val kb = bitsPerSec / 1000.0
            val mb = kb / 1000.0
            val gb = mb / 1000.0

            when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f Gbps", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f Mbps", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f Kbps", kb)
                else -> "${bitsPerSec.toLong()} bps"
            }
        }
    }

    fun formatDataSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val tb = gb / 1024.0

        return when {
            tb >= 1.0 -> String.format(Locale.getDefault(), "%.2f TB", tb)
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun getStatusBarSpeedParts(bytesPerSec: Long, useBits: Boolean = false): Pair<String, String> {
        return if (!useBits) {
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            when {
                gb >= 1.0 -> Pair(String.format(Locale.US, "%.1f", gb), "G")
                mb >= 1.0 -> {
                    if (mb >= 10.0) {
                        Pair(String.format(Locale.US, "%.0f", mb), "M")
                    } else {
                        Pair(String.format(Locale.US, "%.1f", mb), "M")
                    }
                }
                kb >= 1.0 -> {
                    if (kb >= 10.0) {
                        Pair(String.format(Locale.US, "%.0f", kb), "K")
                    } else {
                        Pair(String.format(Locale.US, "%.1f", kb), "K")
                    }
                }
                else -> Pair(bytesPerSec.toString(), "B")
            }
        } else {
            val bitsPerSec = bytesPerSec * 8.0
            val kb = bitsPerSec / 1000.0
            val mb = kb / 1000.0
            val gb = mb / 1000.0

            when {
                gb >= 1.0 -> Pair(String.format(Locale.US, "%.1f", gb), "G")
                mb >= 1.0 -> {
                    if (mb >= 10.0) {
                        Pair(String.format(Locale.US, "%.0f", mb), "M")
                    } else {
                        Pair(String.format(Locale.US, "%.1f", mb), "M")
                    }
                }
                kb >= 1.0 -> {
                    if (kb >= 10.0) {
                        Pair(String.format(Locale.US, "%.0f", kb), "K")
                    } else {
                        Pair(String.format(Locale.US, "%.1f", kb), "K")
                    }
                }
                else -> Pair(bitsPerSec.toLong().toString(), "b")
            }
        }
    }
}
