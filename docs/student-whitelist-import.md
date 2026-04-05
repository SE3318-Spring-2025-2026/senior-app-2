# Öğrenci beyaz listesi — CSV içe aktarma

## Örnek dosya

Repoda örnek şablon: [`ornek-ogrenci-beyaz-liste.csv`](./ornek-ogrenci-beyaz-liste.csv).

| Sütun | Açıklama |
|--------|-----------|
| `ogrenci_no` | Zorunlu (içe aktarımda kullanılan tek sütun). Öğrenci numarası veya benzersiz kimlik metni. |
| `githuba_giris` | **Yalnızca örnek / takip.** `yapıldı` veya `yapılmadı` yazabilirsiniz; uygulama yüklerken bu sütunu **yok sayar**. Gerçek durum SeniorApp’te **Koordinatör → Student Whitelist** ekranında listelenir: öğrenci GitHub ile ilk kez giriş yaptığında satır **Yapıldı** olarak görünür. |

## İçe aktarma kuralları

- Dosya **UTF-8** `.csv` olmalı.
- İlk satır başlık olabilir (`ogrenci_no`, `student_id`, `id` vb. tanınır ve atlanır).
- Her satırda **ilk sütun** öğrenci numarası olarak okunur; virgülden sonraki sütunlar içe aktarımda kullanılmaz.
- Aynı dosyada tekrarlayan numaralar tekilleştirilir; zaten listede olanlar atlanır.

## GitHub girişi

Öğrenci beyaz listedeyken GitHub OAuth ile hesap oluşturup bağladığında kayıt sistemde **hesaba bağlı** olur. Arayüzde bu, **GitHub girişi: Yapıldı** olarak gösterilir; henüz giriş yapmamışsa **Yapılmadı**. Bağlı kayıtlar güvenlik nedeniyle silinemez.
