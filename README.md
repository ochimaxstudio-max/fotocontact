# FotoContact

Aplikasi Android untuk menampilkan **foto khusus + nama alias + nada khusus** milik Anda
sendiri ketika ada:

| Fitur | Sumber | Cara kerja |
|---|---|---|
| Panggilan suara SIM | Jaringan seluler | Deteksi status telepon `RINGING`, lalu tampilkan layar foto |
| Panggilan suara WhatsApp | WhatsApp | Baca notifikasi panggilan WhatsApp, tampilkan layar foto di atasnya |
| Panggilan video WhatsApp | WhatsApp | Sama, dibedakan lewat kata kunci notifikasi |
| Pesan WhatsApp ("intip pesan") | WhatsApp | Foto + baris pertama pesan muncul di layar kunci, **status tetap belum dibaca** |

Setiap kontak bisa punya: foto panggilan (9:16), foto pesan (1:1), nama alias,
dan **nada berbeda untuk tiap fitur** (SIM / WA suara / WA video / WA pesan).

---

## 1. Cara mendapatkan file .apk

Proyek ini adalah **kode sumber lengkap**. Pilih salah satu cara berikut untuk menghasilkan `.apk`.

### Cara A — GitHub Actions (tanpa install apa pun, paling mudah)

1. Buat repositori baru di GitHub (boleh private).
2. Unggah seluruh isi folder ini ke repositori tersebut (bisa lewat tombol **Add file → Upload files**).
3. Buka tab **Actions** → pilih workflow **Build APK** → **Run workflow**.
4. Tunggu 3–6 menit. Setelah selesai, buka hasil build → bagian **Artifacts** →
   unduh `FotoContact-debug-apk`.
5. Ekstrak zip-nya, salin `app-debug.apk` ke ponsel, lalu pasang manual
   (aktifkan "Instal aplikasi tidak dikenal" untuk aplikasi File/Chrome).

APK debug sudah ditandatangani dengan kunci debug, jadi bisa langsung dipasang.

### Cara B — Android Studio

1. Buka Android Studio → **Open** → pilih folder ini.
2. Tunggu Gradle sync selesai (butuh koneksi internet untuk mengunduh dependensi).
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. File ada di `app/build/outputs/apk/debug/app-debug.apk`.

### Cara C — Baris perintah

```bash
# butuh JDK 17 + Android SDK (platform 34, build-tools 34)
gradle assembleDebug
```

---

## 2. Pengaturan setelah dipasang

Buka aplikasi → menu **Izin & Status**, aktifkan berurutan:

1. **Tampil di atas aplikasi lain** — wajib.
2. **Akses notifikasi** — wajib untuk semua fitur WhatsApp.
3. **Izin telepon, log panggilan, kontak** — wajib untuk panggilan SIM
   (tanpa izin log panggilan, Android tidak memberikan nomor pemanggil).
4. **Izin notifikasi** (Android 13+) — cadangan untuk layar terkunci.
5. **Bebaskan dari hemat baterai** — sangat disarankan.
6. **Akses Jangan Ganggu** — hanya jika ingin membisukan dering bawaan.
7. **Notifikasi layar penuh** (Android 14+) — disarankan.

Untuk Xiaomi/Redmi/POCO, Oppo, Vivo, Realme: aktifkan juga **Autostart** dan izin
**"Tampilkan jendela pop-up saat berjalan di latar belakang"** serta
**"Tampilkan di layar kunci"** di pengaturan aplikasi bawaan ponsel.
Tanpa itu, tampilan tidak akan muncul saat layar mati.

### Menambahkan kontak

Tombol **+** → isi:

- **Nama alias** — teks yang muncul di layar.
- **Nama di WhatsApp** — harus **sama persis** dengan yang tampil di notifikasi WhatsApp
  (biasanya nama kontak di buku telepon). Boleh diisi beberapa, dipisah koma.
- **Nomor telepon** — untuk panggilan SIM. Format bebas (`0812…` atau `+62812…`),
  pencocokan memakai 9 digit terakhir.
- **Foto panggilan** dan **foto pesan** — pilih dari galeri, lalu atur lewat layar potong
  (geser, cubit untuk zoom, tombol putar).
- Hidupkan fitur yang diinginkan dan pilih nada untuk masing-masing.

Tombol **Uji panggilan** / **Uji pesan** untuk melihat hasilnya tanpa menunggu telepon masuk.

### Agar nada tidak dobel

FotoContact tidak bisa mematikan nada WhatsApp dari luar. Dua pilihan:

- **Disarankan**: buka chat kontak itu di WhatsApp → **Notifikasi khusus** →
  atur nada pesan/panggilan ke **None/Senyap**. Nada FotoContact yang akan terdengar.
- Atau hidupkan **Bisukan dering sistem saat FotoContact tampil** di Pengaturan
  (perlu izin Akses Jangan Ganggu; volume dikembalikan otomatis setelah tampilan ditutup).

---

## 3. Yang perlu Anda ketahui (batasan nyata)

Ini penting, supaya harapannya sesuai kenyataan:

1. **Foto WhatsApp tidak diganti, tetapi ditimpa.** Android tidak mengizinkan aplikasi
   pihak ketiga mengubah isi layar panggilan WhatsApp. FotoContact menampilkan layarnya
   sendiri **di atas** layar WhatsApp. Hasil akhirnya sama dari sisi mata Anda, tetapi
   pada beberapa ponsel layar WhatsApp bisa muncul sepersekian detik lebih dulu, atau
   sesekali naik kembali ke depan. Untuk itu ada tombol **Tutup tampilan**, dan opsi
   **Selalu pakai tampilan layar penuh** di Pengaturan bila mode overlay kurang stabil
   di ponsel Anda.
2. **Tombol Jawab/Tolak untuk WhatsApp** memakai tombol aksi dari notifikasi WhatsApp.
   Jika versi WhatsApp Anda tidak menyertakan tombol tersebut pada notifikasi,
   tombol akan disembunyikan dan Anda tetap bisa menutup tampilan lalu menjawab
   dari layar WhatsApp.
3. **Membedakan panggilan video vs suara** memakai kata kunci teks notifikasi.
   Bila bahasa WhatsApp Anda berbeda, ubah daftar kata kunci di **Pengaturan**.
4. **Intip pesan** hanya bisa menampilkan isi pesan yang memang ada di notifikasi.
   Jika pratinjau pesan dimatikan di WhatsApp, atau ponsel baru dinyalakan dan belum
   pernah dibuka kuncinya, isi pesan tidak tersedia. Aplikasi ini **tidak pernah**
   memanggil `cancelNotification` dan tidak membuka chat, jadi status pesan tetap
   belum dibaca selama Anda tidak membuka WhatsApp.
5. **Pesan grup** dikenali bila nama pengirim muncul di notifikasi. Untuk grup,
   pencocokan dilakukan pada nama grup atau nama pengirim di awal teks.
6. **Aplikasi ini memakai izin Akses Notifikasi dan overlay**, sehingga kemungkinan besar
   tidak akan lolos kebijakan Google Play. Itu sebabnya dipakai lewat pemasangan manual.
7. **Nada khusus per kontak untuk panggilan SIM** juga tersedia bawaan di Android
   (Kontak → Edit → Nada dering). Anda boleh memakai itu dan mematikan nada di FotoContact.

## 4. Struktur kode

```
app/src/main/java/com/fotocontact/app/
├── App.kt                       kanal notifikasi
├── data/                        Rule, FeatureCfg, RuleStore (JSON), Prefs
├── service/
│   ├── WaNotificationListener   membaca notifikasi WhatsApp (panggilan + pesan)
│   └── PhoneStateReceiver       mendeteksi panggilan SIM masuk
├── overlay/
│   ├── OverlayCoordinator       memilih mode tampilan, nada, dan penutupan
│   ├── CallOverlayActivity      layar penuh di atas layar kunci
│   ├── WindowOverlay            jendela overlay saat layar tidak terkunci
│   ├── PeekActivity/PeekAdapter tampilan intip pesan
│   └── CallActions              jawab/tolak panggilan
├── ui/                          MainActivity, EditRuleActivity, CropActivity, CropView,
│                                SetupActivity, SettingsActivity
└── util/                        Photos, RingtonePlayer, Matcher, Perms
```

Tidak ada data yang dikirim ke mana pun. Semua foto dan pengaturan disimpan di
penyimpanan internal aplikasi.
