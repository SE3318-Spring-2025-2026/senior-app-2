package com.seniorapp.entity;

/**
 * Bir kullanıcı hesabının yaşam döngüsü durumunu temsil eder.
 *
 * <ul>
 *   <li>{@link #ACTIVE}    – Hesap aktif ve giriş yapabilir.</li>
 *   <li>{@link #PENDING}   – Hesap oluşturuldu ancak henüz onaylanmadı.</li>
 *   <li>{@link #DISBANDED} – Hesap devre dışı bırakıldı / pasifleştirildi.</li>
 * </ul>
 */
public enum UserStatus {
    /** Hesap tam erişime sahip, aktif durumda. */
    ACTIVE,

    /** Hesap oluşturuldu ama koordinatör onayı bekleniyor. */
    PENDING,

    /** Hesap kapatıldı ya da mezuniyet gibi bir nedenle pasifleştirildi. */
    DISBANDED
}
