package mg.sitraka.lkl;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Baiboly Malagasy 1865 (SQLite). Table: verses(book, chapter, verse, text).
 * Numéros de livre ×10 (Gen=10 ... Apok=730).
 *
 * Mamaly: "Sal. 82" (toko manontolo), "Jao. 3:16", "Eks. 17:14-16",
 * "Jao. 20:30,31", "Eks. 17:14; Jao. 20:30,31; 1 Kôr. 10:11" (maromaro).
 */
public class BibleDb {

    private static SQLiteDatabase sDb = null;
    private static boolean sTried = false;

    public static synchronized SQLiteDatabase get(Context ctx) {
        if (sTried) return sDb;
        sTried = true;
        try {
            File dbFile = new File(ctx.getFilesDir(), "bible_mg65.db");
            if (!dbFile.exists()) {
                InputStream is = ctx.getAssets().open("bible_mg65.db");
                FileOutputStream fos = new FileOutputStream(dbFile);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                fos.close();
                is.close();
            }
            sDb = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            sDb = null;
        }
        return sDb;
    }

    // alias..., numéro (le dernier élément)
    private static final String[][] BOOK_MAP = {
        {"Gen", "Jen", "Genesisy", "10"},
        {"Eks", "Exo", "Eksodosy", "20"},
        {"Lev", "Levitikosy", "30"},
        {"Nom", "Nomery", "40"},
        {"Deo", "Det", "Deotoronomia", "50"},
        {"Jos", "Josoa", "60"},
        {"Mpi", "Mpits", "Mpitsara", "70"},
        {"Rot", "Rota", "80"},
        {"1Sa", "1Sam", "1Samoela", "90"},
        {"2Sa", "2Sam", "2Samoela", "100"},
        {"1Mp", "1Mpanj", "1Mpanjaka", "110"},
        {"2Mp", "2Mpanj", "2Mpanjaka", "120"},
        {"1Ta", "1Tant", "1Tantara", "130"},
        {"2Ta", "2Tant", "2Tantara", "140"},
        {"Ezr", "Ezra", "150"},
        {"Neh", "Nehemia", "160"},
        {"Est", "Estera", "190"},
        {"Job", "Joba", "220"},
        {"Slm", "Sal", "Psa", "Salamo", "230"},
        {"Ohab", "Pro", "Ohabolana", "240"},
        {"Mpit", "Mpito", "Ecc", "Mpitoriteny", "250"},
        {"Ton", "Tonon", "Son", "260"},
        {"Isa", "Isaia", "290"},
        {"Jer", "Jeremia", "300"},
        {"Fit", "Lam", "Fitomaniana", "310"},
        {"Ezek", "Eze", "Ezekiela", "330"},
        {"Dan", "Daniela", "340"},
        {"Hos", "Oz", "Hosea", "350"},
        {"Joel", "Joela", "360"},
        {"Amos", "Amo", "Amosa", "370"},
        {"Obad", "Abd", "Oba", "Obadia", "380"},
        {"Jona", "390"},
        {"Mika", "Mik", "400"},
        {"Nah", "Nahoma", "410"},
        {"Hab", "Habakoka", "420"},
        {"Zef", "Sef", "Zefania", "430"},
        {"Hag", "Hagay", "440"},
        {"Zak", "Zakaria", "450"},
        {"Mal", "Malakia", "460"},
        {"Mat", "Mt", "Matio", "470"},
        {"Mark", "Mar", "Mk", "Marka", "480"},
        {"Liok", "Lio", "Lk", "Lioka", "490"},
        {"Joan", "Jao", "Jn", "Joany", "500"},
        {"Asan", "Asa", "Act", "Apostoly", "510"},
        {"Rom", "Ro", "Romana", "520"},
        {"1Kor", "1Ko", "530"},
        {"2Kor", "2Ko", "540"},
        {"Gal", "Galatiana", "550"},
        {"Efez", "Efes", "Efe", "Efeziana", "560"},
        {"Fil", "Filipiana", "570"},
        {"Kol", "Kolosiana", "580"},
        {"1Tes", "1Te", "590"},
        {"2Tes", "2Te", "600"},
        {"1Tim", "1Ti", "610"},
        {"2Tim", "2Ti", "620"},
        {"Tito", "Tit", "630"},
        {"Flm", "Filemon", "Filemona", "Fim", "640"},
        {"Hebr", "Heb", "Hebreo", "650"},
        {"Jak", "Jakoba", "660"},
        {"1Pie", "1Pet", "1Pe", "670"},
        {"2Pie", "2Pet", "2Pe", "680"},
        {"1Jo", "1Jao", "1Jn", "690"},
        {"2Jo", "2Jao", "2Jn", "700"},
        {"3Jo", "3Jao", "3Jn", "710"},
        {"Joda", "Juda", "Jod", "720"},
        {"Apok", "Apo", "Rev", "Apokalipsa", "730"}
    };

    static String norm(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[\\s.'’\\-]", "");
    }

    public static int bookNum(String name) {
        String n = norm(name);
        if (n.isEmpty()) return -1;
        // 1) correspondance exacte
        for (String[] e : BOOK_MAP)
            for (int k = 0; k < e.length - 1; k++)
                if (norm(e[k]).equals(n)) return Integer.parseInt(e[e.length - 1]);
        if (n.length() < 3) return -1;
        // 2) préfixe d'un nom connu (ex. "mpitor" -> Mpitoriteny)
        for (String[] e : BOOK_MAP)
            for (int k = 0; k < e.length - 1; k++)
                if (norm(e[k]).startsWith(n)) return Integer.parseInt(e[e.length - 1]);
        // 3) le texte commence par une abréviation connue (≥3 car.)
        for (String[] e : BOOK_MAP)
            for (int k = 0; k < e.length - 1; k++) {
                String a = norm(e[k]);
                if (a.length() >= 3 && n.startsWith(a)) return Integer.parseInt(e[e.length - 1]);
            }
        return -1;
    }

    private static final Pattern REF = Pattern.compile(
        "^\\s*((?:[1-3]\\s*)?\\p{L}[\\p{L}'’]*)\\.?\\s*(\\d+)(?:\\s*:\\s*([0-9,\\-–\\s]+))?\\s*$");
    private static final Pattern REF_NOBOOK = Pattern.compile(
        "^\\s*(\\d+)\\s*:\\s*([0-9,\\-–\\s]+)\\s*$");

    /** Vrai si le texte ressemble à une référence dont le livre est connu. */
    public static boolean isRef(String text) {
        if (text == null) return false;
        String first = text.split(";")[0];
        Matcher m = REF.matcher(first);
        return m.matches() && bookNum(m.group(1)) > 0;
    }

    /** Retourne une liste de {libellé, texte} pour chaque référence. */
    public static List<String[]> lookupAll(Context ctx, String refText) {
        List<String[]> out = new ArrayList<>();
        SQLiteDatabase db = get(ctx);
        if (db == null || refText == null) return out;
        String lastBook = null;
        for (String raw : refText.split(";")) {
            String part = raw.trim();
            if (part.isEmpty()) continue;
            String book;
            int ch;
            String verses;
            Matcher m = REF.matcher(part);
            if (m.matches()) {
                book = m.group(1);
                ch = Integer.parseInt(m.group(2));
                verses = m.group(3);
                lastBook = book;
            } else {
                Matcher m2 = REF_NOBOOK.matcher(part);
                if (lastBook != null && m2.matches()) {
                    book = lastBook;
                    ch = Integer.parseInt(m2.group(1));
                    verses = m2.group(2);
                } else {
                    out.add(new String[]{part, ""});
                    continue;
                }
            }
            int bk = bookNum(book);
            if (bk < 0) { out.add(new String[]{part, ""}); continue; }
            out.add(new String[]{part, fetch(db, bk, ch, verses)});
        }
        return out;
    }

    private static String fetch(SQLiteDatabase db, int bk, int ch, String verses) {
        List<int[]> ranges = new ArrayList<>();
        if (verses == null || verses.trim().isEmpty()) {
            ranges.add(new int[]{1, 999});
        } else {
            for (String tok : verses.split(",")) {
                tok = tok.trim();
                if (tok.isEmpty()) continue;
                try {
                    String[] ab = tok.split("[\\-–]");
                    int a = Integer.parseInt(ab[0].trim());
                    int b = ab.length > 1 && !ab[1].trim().isEmpty() ? Integer.parseInt(ab[1].trim()) : a;
                    ranges.add(new int[]{a, b});
                } catch (Exception ignored) { }
            }
        }
        if (ranges.isEmpty()) return "";
        List<String> texts = new ArrayList<>();
        List<Integer> nums = new ArrayList<>();
        for (int[] r : ranges) {
            Cursor c = null;
            try {
                c = db.rawQuery(
                    "SELECT verse,text FROM verses WHERE book=? AND chapter=? AND verse BETWEEN ? AND ? ORDER BY verse",
                    new String[]{String.valueOf(bk), String.valueOf(ch), String.valueOf(r[0]), String.valueOf(r[1])});
                while (c.moveToNext()) {
                    nums.add(c.getInt(0));
                    texts.add(c.getString(1));
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.close();
            }
        }
        boolean multi = texts.size() > 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i).replaceAll("\\[[^\\]]*\\]\\s*", "");
            if (sb.length() > 0) sb.append(multi ? "\n" : " ");
            if (multi) sb.append(nums.get(i)).append("  ");
            sb.append(t);
        }
        return sb.toString();
    }

    /** Compat: premier résultat uniquement. */
    public static String lookup(Context ctx, String ref) {
        List<String[]> r = lookupAll(ctx, ref);
        return r.isEmpty() ? "" : r.get(0)[1];
    }
}
