package com.example.myproject.service.helper;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberHelper {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    // 8 chiffres locaux commençant par 3, 2 ou 4
    private static final Pattern MAURITANIAN_LOCAL = Pattern.compile("^[324]\\d{7}$");
    // préfixes +222, 222 ou 00222 suivis de 8 chiffres locaux
    private static final Pattern MAURITANIAN_WITH_PREFIX = Pattern.compile("^(?:\\+222|222|00222)([324]\\d{7})$");
    // numéro international au format E.164 (avec ou sans +)
    private static final Pattern INTERNATIONAL_E164 = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    /**
     * Vérifie qu'un numéro est valide :
     * - soit mauritanien : local (8 chiffres débutant par 3/2/4)
     *   ou avec l'un des préfixes +222, 222, 00222
     * - soit international au format E.164
     */
    public static boolean isValidPhoneNumber(String raw) {
        if (raw == null) return false;
        String cleaned = raw.trim().replaceAll("\\s+", "");
        if (MAURITANIAN_LOCAL.matcher(cleaned).matches()) {
            return true;
        }
        if (MAURITANIAN_WITH_PREFIX.matcher(cleaned).matches()) {
            return true;
        }
        return INTERNATIONAL_E164.matcher(cleaned).matches();
    }

    /**
     * Normalise les numéros mauritaniens en E.164 (+222XXXXXXXX),
     * et supprime simplement les espaces des autres numéros.
     */
    public static String normalizePhoneNumber(String raw) {
        if (raw == null) return null;

        String cleaned = raw.trim().replaceAll("[^\\d+]", ""); // Supprime espaces, tirets, etc.

        // 1️⃣ Si commence par + → déjà international, on nettoie
        if (cleaned.startsWith("+")) {
            return "+" + cleaned.replaceAll("[^\\d]", "");
        }

        // 2️⃣ Si commence par 00 → remplacer par +
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }

        // 3️⃣ Mauritanie : numéro local 3XXXXXXX → +2223XXXXXXX
        if (cleaned.matches("^3\\d{7}$")) {
            return "+222" + cleaned;
        }

        // 4️⃣ Maroc : commence par 06, 07, 05 → +212XXXX
        if (cleaned.matches("^0[5-7]\\d{8}$")) {
            return "+212" + cleaned.substring(1);
        }

        // 5️⃣ Si déjà commence par indicatif (ex: 212...) → on ajoute juste +
        if (cleaned.matches("^(212|221|33|225|237|...).*")) { // ajoute d'autres préfixes selon tes besoins
            return "+" + cleaned;
        }

        // 6️⃣ Par défaut : retour sans modifier, juste suppression des espaces
        return cleaned;
    }
}
