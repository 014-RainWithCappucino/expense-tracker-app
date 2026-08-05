package com.nijika21.yourmoney.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Every string in the first four tests was captured off the device on
 * 2026-08-06, verbatim except for the account holder's own name. Nothing here is
 * invented — that is the point of shipping M1 before M3.
 */
class TransactionSignalTest {

    private fun signal(title: String?, text: String?, bigText: String? = null) =
        TransactionSignal.classify(title, text, bigText)

    @Test
    fun `real GoPay transfer out is a transaction`() {
        assertEquals(
            CaptureSignal.TRANSAKSI,
            signal("Transfer berhasil", "Rp8.127 udah dikirim ke BCA NAMA PENERIMA."),
        )
    }

    @Test
    fun `real GoPay payment is a transaction`() {
        assertEquals(
            CaptureSignal.TRANSAKSI,
            signal(
                "Pembayaran berhasil!",
                "Mantaap, kamu barusan bayar Rp26.374 ke PT KEPO SOLUSI INDONESIA.",
            ),
        )
    }

    @Test
    fun `real BCA incoming transfer is a transaction`() {
        assertEquals(
            CaptureSignal.TRANSAKSI,
            signal(
                "BCA mobile",
                "Financial Diary: Pemasukan sebesar IDR 8,127.00 dari ***PET **AK ***GSA " +
                    "di kategori Transfer Rekening.",
            ),
        )
    }

    /**
     * The one the user named specifically. It has a number (26), a brand, and an
     * exclamation mark, and it is worth nothing: "GoPay Coins" is not money, so
     * the bare `26` must never read as an amount.
     */
    @Test
    fun `GoPay coins promo is not a transaction`() {
        assertEquals(
            CaptureSignal.BUKAN,
            signal(
                "Criiing! Kamu dapet GoPay Coins 💰",
                "Cihuy, kamu dapet 26 GoPay Coins! Klik buat liat detailnya.",
            ),
        )
    }

    @Test
    fun `a promo with a real rupiah amount is still not a transaction`() {
        assertEquals(
            CaptureSignal.BUKAN,
            signal("Diskon spesial!", "Voucher diskon sampai Rp50.000 buat kamu. Buruan pakai!"),
        )
    }

    /**
     * The asymmetry, asserted so it cannot be "tidied" later: reward copy tacked
     * onto a genuine receipt must not veto the receipt.
     */
    @Test
    fun `reward copy appended to a real receipt stays a transaction`() {
        assertEquals(
            CaptureSignal.TRANSAKSI,
            signal("Pembayaran berhasil!", "Kamu bayar Rp15.000. Bonus, kamu dapet 10 poin!"),
        )
    }

    @Test
    fun `an amount with no completion wording is uncertain, not discarded`() {
        assertEquals(
            CaptureSignal.MUNGKIN,
            signal("BCA mobile", "Rp250.000 — rincian ada di aplikasi."),
        )
    }

    /**
     * The names here are stand-ins. Social and chat notifications are the bulk of
     * what the listener sees, and committing a captured one would put real
     * people's names in the repo for no test value — the shape is what matters,
     * not who sent it.
     */
    @Test
    fun `chat and delivery notifications carry no amount`() {
        assertEquals(CaptureSignal.BUKAN, signal("Gojek", "Driver kamu sudah sampai."))
        assertEquals(
            CaptureSignal.BUKAN,
            signal("Nama Orang", "Anda mendapat 37 notifikasi baru dari A, B dan lainnya."),
        )
    }

    @Test
    fun `IDR and Rp are both recognised, with or without a space`() {
        val shapes = listOf(
            "Rp8.127 dikirim",
            "Rp 8.127 dikirim",
            "IDR 8,127.00 diterima",
            "idr8127 diterima",
            "RP8.127 dikirim",
        )
        for (shape in shapes) {
            assertEquals(shape, CaptureSignal.TRANSAKSI, signal(null, shape))
        }
    }

    @Test
    fun `bigText is searched too, since BCA puts the amount there`() {
        assertEquals(
            CaptureSignal.TRANSAKSI,
            signal(
                title = "BCA mobile",
                text = "Financial Diary",
                bigText = "Pengeluaran sebesar IDR 26,374.00 di kategori Belanja.",
            ),
        )
    }

    @Test
    fun `empty input is not a transaction`() {
        assertEquals(CaptureSignal.BUKAN, signal(null, null, null))
        assertEquals(CaptureSignal.BUKAN, signal("", "", ""))
    }
}
