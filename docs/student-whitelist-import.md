# Öğrenci beyaz listesi — CSV içe aktarma

## Örnek dosya

Repoda şablon: [`ornek-ogrenci-beyaz-liste.csv`](./ornek-ogrenci-beyaz-liste.csv).

CSV’de **yalnızca öğrenci numarası** olmalı: tercihen tek sütun (`ogrenci_no` başlığı veya başlıksız, her satırda bir numara). Yeni eklenen kayıtlarda GitHub henüz yoktur; **GitHub girişi: Yapıldı / Yapılmadı** bilgisi yalnızca uygulamadaki listede, öğrenci giriş yaptıkça güncellenir — CSV’ye ayrıca yazılmaz.

## Kurallar

- Dosya **UTF-8** ve uzantı **`.csv`**.
- İsteğe bağlı ilk satır başlık: `ogrenci_no`, `student_id`, `studentid`, `id`, `numara` vb. tanınıp atlanır.
- Her satırda **sadece ilk sütun** okunur; yanlışlıkla eklenmiş ek sütunlar yok sayılır.
- Tek sütunlu dosyada tüm satırlar öğrenci numarasıdır.
- Aynı dosyada tekrarlayan numaralar tekilleştirilir; zaten beyaz listede olanlar atlanır.

## Liste ekranı

Koordinatör panelindeki tabloda **GitHub girişi** sütunu canlı durumu gösterir. Bağlı (giriş yapılmış) satırlar güvenlik nedeniyle silinemez.
