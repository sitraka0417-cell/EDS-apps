package mg.sitraka.lkl;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;
import android.view.ViewConfiguration;
import android.view.VelocityTracker;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Shader;
import android.graphics.LinearGradient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lesona Kilasy Lehibe — lecteur de leçons de l'École du Sabbat en malgache.
 * Données : API Adventech v1 (JSON), mises en cache pour une lecture hors ligne.
 */
public class MainActivity extends Activity {

    // ── Couleurs ───────────────────────────────────────────────────────────
    static final int BAR_SOLID = 0xFF15161D, DARK_SCRIM = 0xFF15161D;
    int C_BG, C_BAR, C_CARD, C_SHEET, C_LINE, C_ACCENT, C_LINK, C_QLINK, C_TEXT, C_MUTED, C_GOLD, C_QBOX, C_NOTE;
    boolean mDark = true;

    void applyAppTheme(boolean dark) {
        mDark = dark;
        if (dark) {
            C_BG = 0xFF0E0F14; C_BAR = 0xFF15161D; C_CARD = 0xFF1B1C25; C_SHEET = 0xFF1C1D27;
            C_LINE = 0xFF2B2C3A; C_ACCENT = 0xFF8B8BF5; C_LINK = 0xFF7C83FF; C_QLINK = 0xFF38BDF8;
            C_TEXT = 0xFFECECF1; C_MUTED = 0xFF9A9BAA; C_GOLD = 0xFFD4A017; C_QBOX = 0xFF101A2E;
            C_NOTE = 0xFF1A2540;
        } else {
            C_BG = 0xFFF3F4F9; C_BAR = 0xFFFFFFFF; C_CARD = 0xFFFFFFFF; C_SHEET = 0xFFFFFFFF;
            C_LINE = 0xFFE0E3EC; C_ACCENT = 0xFF5B5BD6; C_LINK = 0xFF4F46E5; C_QLINK = 0xFF0369A1;
            C_TEXT = 0xFF1B1C25; C_MUTED = 0xFF6B6F80; C_GOLD = 0xFFB7791F; C_QBOX = 0xFFE6EFFC;
            C_NOTE = 0xFFEAEEF7;
        }
        if (mRoot != null) mRoot.setBackgroundColor(C_BG);
    }

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // ── API / cache ────────────────────────────────────────────────────────
    static final String API = "https://sabbath-school.adventech.io/api/v1/mg/quarterlies/";
    static final long LIST_AGE = 6L * 3600 * 1000;
    static final long STRUCT_AGE = 12L * 3600 * 1000;
    static final long FOREVER = Long.MAX_VALUE;

    static final String[] SOKAJY_NAMES = {"Lesona Lehibe", "Lesona Tanora Zokiny", "Lesona Zatovo"};
    static final boolean[] HAS_TSIANJERY = {true, false, true};
    static final String[] DAYS_MG = {"Alahady", "Alatsinainy", "Talata", "Alarobia", "Alakamisy", "Zoma", "Asabotsy"};
    static final String[] MONTHS_MG = {"Janoary", "Febroary", "Marsa", "Aprily", "May", "Jona", "Jolay",
            "Aogositra", "Septambra", "Oktobra", "Novambra", "Desambra"};

    static final Pattern BLOCK = Pattern.compile("(?is)<(h[1-6]|p|blockquote|li)(\\s[^>]*)?>(.*?)</\\1>");
    static final Pattern VERSE_A = Pattern.compile(
            "(?is)<a\\b[^>]*class=[\"'][^\"']*\\bverse\\b[^\"']*[\"'][^>]*>(.*?)</a>");
    static final Pattern BIBLE_PAT = Pattern.compile(
            "((?:[1-3]\\s*)?\\p{L}{2,14}\\.?\\s+\\d+:\\d+(?:\\s*[,\\-–]\\s*\\d+)*)");
    static final Pattern REF_FULL = Pattern.compile(
            "(?:[1-3]\\s*)?\\p{L}[\\p{L}'’]{1,14}\\.?\\s*\\d+\\s*:\\s*\\d+(?:\\s*[,\\-–]\\s*\\d+)*");

    // ── Modèles ────────────────────────────────────────────────────────────
    static class Quarter { String id = "", title = "", cover = "", human = "", start = "", end = "", desc = ""; }
    static class Lesson { String id = "", title = "", cover = "", start = "", end = ""; }
    static class Day { String id = "", title = "", date = ""; }

    // ── État ───────────────────────────────────────────────────────────────
    Handler mHandler;
    FrameLayout mRoot;
    SharedPreferences mPrefs;
    int mSokajy = 0, mPage = 0, mPageToken = 0, mTextSp = 18;
    String mQuarterId = "", mLessonId = "";
    int mDayNum = 1;
    List<Quarter> mQuarters = new ArrayList<>();
    volatile List<Lesson> mLessons = new ArrayList<>();
    volatile String mLessonsQid = "";
    volatile List<Day> mDays = new ArrayList<>();
    volatile String mDaysKey = "", mLessonCover = "";
    String mDayHtml = null, mDayKey = "";
    LinearLayout mReaderContent, mDotsRow;
    GestureDetector mGesture;
    final ExecutorService mImgPool = Executors.newFixedThreadPool(3);
    volatile String mLastErr = "";
    // thème de lecture (Zatovo = en couleur, tahaka ny app ofisialy)
    boolean mYouth = false;
    int mQColorP = 0;
    int mThText, mThLink, mThAccent, mThMuted;
    volatile String mShareText = "";

    // sync
    final AtomicBoolean mSyncing = new AtomicBoolean(false);
    volatile boolean mSyncHidden = false, mSyncDidWork = false;
    Dialog mSyncDlg;
    RingView mSyncRing;
    TextView mSyncTxt;

    // ══════════════════════════════════════════════════════════════════════
    // CYCLE DE VIE
    // ══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(0xFF000000);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mRoot = (FrameLayout) findViewById(R.id.root_container);
        mHandler = new Handler(Looper.getMainLooper());
        mPrefs = getSharedPreferences("lkl_prefs", MODE_PRIVATE);
        mSokajy = mPrefs.getInt("sokajy", 0);
        mTextSp = mPrefs.getInt("tsize", 18);
        System.setProperty("http.maxConnections", "16");
        applyAppTheme(mPrefs.getBoolean("dark", true));
        applyTheme(false, 0);

        mGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX(), dy = e2.getY() - e1.getY();
                if (mDrawer != null) {
                    if (dx < -dp(60) && Math.abs(dx) > 1.5f * Math.abs(dy)) closeDrawer();
                    return false;
                }
                if (mPage != 3) return false;
                if (Math.abs(dx) > dp(90) && Math.abs(dx) > 2 * Math.abs(dy) && Math.abs(vx) > dp(250)) {
                    swipeDay(dx < 0 ? 1 : -1);
                }
                return false;
            }
        });
        buildHome(null);
        mHandler.postDelayed(new Runnable() {
            @Override public void run() { startAutoSync(false); }
        }, 900);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ((mPage == 3 || mDrawer != null) && mGesture != null) mGesture.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (mDrawer != null) { closeDrawer(); return; }
        if (mPage == 3 || mPage == 5) { buildHome(mQuarterId); return; }
        super.onBackPressed();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UTILITAIRES UI
    // ══════════════════════════════════════════════════════════════════════
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    int statusH() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    GradientDrawable rr(int fill, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(r);
        return g;
    }

    GradientDrawable rrs(int fill, int r, int stroke, int sc) {
        GradientDrawable g = rr(fill, r);
        g.setStroke(stroke, sc);
        return g;
    }

    TextView tv(String t, float sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }

    LinearLayout.LayoutParams lpm(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    void roundImage(ImageView iv, final int radiusPx) {
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radiusPx);
            }
        });
        iv.setClipToOutline(true);
    }

    View errBox(String msg, final Runnable retry) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(40), dp(24), dp(40));
        TextView t = tv(msg, 15, C_MUTED, false);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        if (retry != null) {
            TextView b = tv("Andramo indray", 15, C_TEXT, true);
            b.setGravity(Gravity.CENTER);
            b.setBackgroundDrawable(rr(0x338B8BF5, dp(24)));
            b.setPadding(dp(28), dp(12), dp(28), dp(12));
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { retry.run(); }
            });
            box.addView(b, lpm(WRAP, WRAP, 0, dp(18), 0, 0));
        }
        return box;
    }

    // ── Icônes dessinées (aucun fichier image) ─────────────────────────────
    static class Ico extends View {
        static final int MENU = 0, FILTER = 1, BACK = 2, CAL = 3, CHEV = 4, AA = 5, SHARE = 6, BOOK = 7, REFRESH = 8, MOON = 9, SUN = 10, INFO = 11;
        final int type;
        int color = 0xFFFFFFFF;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();

        Ico(Context c, int type) { super(c); this.type = type; }

        Ico color(int c) { color = c; return this; }

        @Override
        protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight());
            float u = s / 24f;
            cv.save();
            cv.translate((getWidth() - s) / 2f, (getHeight() - s) / 2f);
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f * u);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            switch (type) {
                case MENU:
                    cv.drawLine(4 * u, 6 * u, 20 * u, 6 * u, p);
                    cv.drawLine(4 * u, 12 * u, 20 * u, 12 * u, p);
                    cv.drawLine(4 * u, 18 * u, 20 * u, 18 * u, p);
                    break;
                case FILTER:
                    cv.drawLine(3 * u, 6 * u, 21 * u, 6 * u, p);
                    cv.drawLine(7 * u, 12 * u, 21 * u, 12 * u, p);
                    cv.drawLine(12 * u, 18 * u, 21 * u, 18 * u, p);
                    break;
                case BACK:
                    cv.drawLine(5 * u, 12 * u, 19 * u, 12 * u, p);
                    path.reset();
                    path.moveTo(11 * u, 6 * u);
                    path.lineTo(5 * u, 12 * u);
                    path.lineTo(11 * u, 18 * u);
                    cv.drawPath(path, p);
                    break;
                case CHEV:
                    path.reset();
                    path.moveTo(9 * u, 5 * u);
                    path.lineTo(16 * u, 12 * u);
                    path.lineTo(9 * u, 19 * u);
                    cv.drawPath(path, p);
                    break;
                case CAL:
                    cv.drawRoundRect(new RectF(4 * u, 6 * u, 20 * u, 20 * u), 3 * u, 3 * u, p);
                    cv.drawLine(4 * u, 11 * u, 20 * u, 11 * u, p);
                    cv.drawLine(8 * u, 3 * u, 8 * u, 8 * u, p);
                    cv.drawLine(16 * u, 3 * u, 16 * u, 8 * u, p);
                    break;
                case AA:
                    p.setStyle(Paint.Style.FILL);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTypeface(Typeface.DEFAULT_BOLD);
                    p.setTextSize(15 * u);
                    cv.drawText("Aa", 12 * u, 17 * u, p);
                    break;
                case SHARE:
                    p.setStrokeWidth(1.6f * u);
                    cv.drawLine(8 * u, 11 * u, 16 * u, 6.5f * u, p);
                    cv.drawLine(8 * u, 13 * u, 16 * u, 17.5f * u, p);
                    p.setStyle(Paint.Style.FILL);
                    cv.drawCircle(18 * u, 5.5f * u, 2.8f * u, p);
                    cv.drawCircle(6 * u, 12 * u, 2.8f * u, p);
                    cv.drawCircle(18 * u, 18.5f * u, 2.8f * u, p);
                    break;
                case BOOK:
                    p.setStrokeWidth(1.8f * u);
                    path.reset();
                    path.moveTo(12 * u, 6 * u);
                    path.cubicTo(9 * u, 4 * u, 5.5f * u, 4 * u, 3 * u, 5 * u);
                    path.lineTo(3 * u, 19 * u);
                    path.cubicTo(5.5f * u, 18 * u, 9 * u, 18 * u, 12 * u, 20 * u);
                    path.cubicTo(15 * u, 18 * u, 18.5f * u, 18 * u, 21 * u, 19 * u);
                    path.lineTo(21 * u, 5 * u);
                    path.cubicTo(18.5f * u, 4 * u, 15 * u, 4 * u, 12 * u, 6 * u);
                    path.lineTo(12 * u, 20 * u);
                    cv.drawPath(path, p);
                    cv.drawLine(15 * u, 9 * u, 18 * u, 9 * u, p);
                    cv.drawLine(15 * u, 12 * u, 18 * u, 12 * u, p);
                    break;
                case REFRESH: {
                    cv.drawArc(new RectF(4 * u, 4 * u, 20 * u, 20 * u), 20, 290, false, p);
                    double ang = Math.toRadians(310);
                    float px = 12 * u + 8 * u * (float) Math.cos(ang), py = 12 * u + 8 * u * (float) Math.sin(ang);
                    float tx = (float) -Math.sin(ang), ty = (float) Math.cos(ang);
                    float nx = -ty, ny = tx;
                    path.reset();
                    path.moveTo(px + tx * 3.4f * u, py + ty * 3.4f * u);
                    path.lineTo(px + nx * 3.2f * u, py + ny * 3.2f * u);
                    path.lineTo(px - nx * 3.2f * u, py - ny * 3.2f * u);
                    path.close();
                    p.setStyle(Paint.Style.FILL);
                    cv.drawPath(path, p);
                    break;
                }
                case MOON: {
                    p.setStyle(Paint.Style.FILL);
                    path.reset();
                    path.addCircle(12 * u, 12 * u, 8 * u, Path.Direction.CW);
                    Path c2 = new Path();
                    c2.addCircle(16.5f * u, 8.5f * u, 6.5f * u, Path.Direction.CW);
                    path.op(c2, Path.Op.DIFFERENCE);
                    cv.drawPath(path, p);
                    break;
                }
                case SUN:
                    cv.drawCircle(12 * u, 12 * u, 4 * u, p);
                    for (int k = 0; k < 8; k++) {
                        double a = Math.toRadians(k * 45);
                        float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                        cv.drawLine(12 * u + 7.2f * u * ca, 12 * u + 7.2f * u * sa,
                                12 * u + 9.8f * u * ca, 12 * u + 9.8f * u * sa, p);
                    }
                    break;
                case INFO:
                    cv.drawCircle(12 * u, 12 * u, 9 * u, p);
                    cv.drawLine(12 * u, 11 * u, 12 * u, 17 * u, p);
                    p.setStyle(Paint.Style.FILL);
                    cv.drawCircle(12 * u, 7.6f * u, 1.3f * u, p);
                    break;
                default:
                    break;
            }
            cv.restore();
        }
    }

    static class FlagView extends View {
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path clip = new Path();

        FlagView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            clip.reset();
            clip.addRoundRect(new RectF(0, 0, w, h), h * 0.2f, h * 0.2f, Path.Direction.CW);
            cv.save();
            cv.clipPath(clip);
            p.setColor(0xFFFFFFFF);
            cv.drawRect(0, 0, w * 0.34f, h, p);
            p.setColor(0xFFFC3D32);
            cv.drawRect(w * 0.34f, 0, w, h / 2f, p);
            p.setColor(0xFF007E3A);
            cv.drawRect(w * 0.34f, h / 2f, w, h, p);
            cv.restore();
        }
    }

    View iconBtn(int type, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundDrawable(rr(0x66000000, dp(14)));
        f.addView(new Ico(this, type), new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        f.setOnClickListener(l);
        return f;
    }

    /** Barre du haut (transparente, devient pleine en défilant). Retourne la ligne de contenu. */
    LinearLayout makeBar(FrameLayout page, final ScrollView scroll, int fadeDp) {
        final FrameLayout bar = new FrameLayout(this);
        bar.setBackgroundColor(0x00000000);
        page.addView(bar, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), statusH() + dp(8), dp(12), dp(8));
        bar.addView(row, new FrameLayout.LayoutParams(MATCH, WRAP));
        final int fade = dp(fadeDp);
        scroll.getViewTreeObserver().addOnScrollChangedListener(new android.view.ViewTreeObserver.OnScrollChangedListener() {
            @Override public void onScrollChanged() {
                float t = Math.min(1f, Math.max(0f, scroll.getScrollY() / (float) fade));
                bar.setBackgroundColor(((int) (255 * t) << 24) | (BAR_SOLID & 0x00FFFFFF));
                View title = bar.findViewWithTag("bartitle");
                if (title != null) title.setAlpha(t);
            }
        });
        return row;
    }

    /** Image d'en-tête plein cadre + dégradé. */
    ImageView addHeroImage(FrameLayout hero) { return addHeroImage(hero, mDark ? C_BG : DARK_SCRIM); }

    ImageView addHeroImage(FrameLayout hero, int endColor) {
        hero.setBackgroundColor(C_CARD);
        if (endColor == DARK_SCRIM) {
            final int r = dp(28);
            hero.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, -r, v.getWidth(), v.getHeight(), r); }
            });
            hero.setClipToOutline(true);
        }
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
        View shade = new View(this);
        shade.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66000000, 0x10000000, (endColor & 0x00FFFFFF) | 0xB3000000, endColor}));
        hero.addView(shade, new FrameLayout.LayoutParams(MATCH, MATCH));
        return iv;
    }

    // ── Boribory fandrosoana (% anatiny) ───────────────────────────────────
    static class RingView extends View {
        final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG),
                txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF box = new RectF();
        float target = 0f, shown = 0f;
        boolean indeterminate = true;
        final long t0 = System.currentTimeMillis();

        RingView(Context c, int accent, int trackColor, int textColor) {
            super(c);
            track.setStyle(Paint.Style.STROKE);
            arc.setStyle(Paint.Style.STROKE);
            arc.setStrokeCap(Paint.Cap.ROUND);
            track.setColor(trackColor);
            arc.setColor(accent);
            txt.setColor(textColor);
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setTypeface(Typeface.DEFAULT_BOLD);
        }

        void setProgress(float p) {
            indeterminate = false;
            target = Math.max(0f, Math.min(1f, p));
            postInvalidateOnAnimation();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float s = Math.min(getWidth(), getHeight());
            float sw = s * 0.085f;
            track.setStrokeWidth(sw);
            arc.setStrokeWidth(sw);
            float pad = sw / 2f + 2f, cx = getWidth() / 2f, cy = getHeight() / 2f;
            box.set(cx - s / 2f + pad, cy - s / 2f + pad, cx + s / 2f - pad, cy + s / 2f - pad);
            cv.drawOval(box, track);
            if (indeterminate) {
                float a = ((System.currentTimeMillis() - t0) % 1100) / 1100f * 360f;
                cv.drawArc(box, a - 90f, 100f, false, arc);
                postInvalidateOnAnimation();
            } else {
                shown += (target - shown) * 0.14f;
                if (Math.abs(target - shown) < 0.002f) shown = target;
                if (shown > 0f) cv.drawArc(box, -90f, 360f * shown, false, arc);
                txt.setTextSize(s * 0.26f);
                cv.drawText(Math.round(shown * 100f) + "%", cx, cy - (txt.ascent() + txt.descent()) / 2f, txt);
                if (shown != target) postInvalidateOnAnimation();
            }
        }
    }

    // ── Feuille glissable (poignée, ou tirer vers le bas en haut du contenu) ──
    class SwipeSheet extends LinearLayout {
        View scroller;
        Runnable onDismiss;
        float downY;
        boolean dragging;
        VelocityTracker vt;
        final int slop = ViewConfiguration.get(MainActivity.this).getScaledTouchSlop();
        boolean fromHandle;

        SwipeSheet(Context c) {
            super(c);
            setOrientation(LinearLayout.VERTICAL);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downY = e.getRawY();
                    dragging = false;
                    fromHandle = e.getY() < dp(44);
                    if (vt != null) vt.recycle();
                    vt = VelocityTracker.obtain();
                    vt.addMovement(e);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (vt != null) vt.addMovement(e);
                    float dy = e.getRawY() - downY;
                    boolean atTop = scroller == null || !scroller.canScrollVertically(-1);
                    if (dy > slop && (fromHandle || atTop)) {
                        dragging = true;
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    if (vt != null) vt.addMovement(e);
                    float dy = e.getRawY() - downY;
                    if (!dragging && dy > slop) dragging = true;
                    if (dragging) setTranslationY(Math.max(0f, dy));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (dragging) {
                        float vy = 0f;
                        if (vt != null) { vt.computeCurrentVelocity(1000); vy = vt.getYVelocity(); }
                        if (getTranslationY() > getHeight() * 0.25f || vy > 1500f) {
                            animate().translationY(getHeight()).setDuration(170).withEndAction(onDismiss).start();
                        } else {
                            animate().translationY(0f).setDuration(170).start();
                        }
                    }
                    dragging = false;
                    if (vt != null) { vt.recycle(); vt = null; }
                    return true;
                }
                default:
                    return true;
            }
        }
    }

    View handleBox() {
        FrameLayout f = new FrameLayout(this);
        View bar = new View(this);
        bar.setBackgroundDrawable(rr(mDark ? 0xFF7C7D8A : 0xFFC4C7D4, dp(3)));
        f.addView(bar, new FrameLayout.LayoutParams(dp(44), dp(5), Gravity.CENTER));
        return f;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DATES
    // ══════════════════════════════════════════════════════════════════════
    Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String[] fmts = {"dd/MM/yyyy", "yyyy-MM-dd"};
        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(s.trim());
            } catch (Exception ignored) { }
        }
        return null;
    }

    Date today0() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    long daysBetween(Date a, Date b) {
        return Math.round((b.getTime() - a.getTime()) / 86400000.0);
    }

    boolean inRange(String start, String end, Date d) {
        Date a = parseDate(start), b = parseDate(end);
        return a != null && b != null && !d.before(a) && !d.after(b);
    }

    String fmtDate(Date d) {
        if (d == null) return "";
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return DAYS_MG[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + " "
                + MONTHS_MG[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }

    // ══════════════════════════════════════════════════════════════════════
    // RÉSEAU + CACHE HORS LIGNE
    // ══════════════════════════════════════════════════════════════════════
    boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    File cacheFile(String rel) { return new File(new File(getFilesDir(), "lkl3"), rel); }

    boolean cacheExists(String rel) { return cacheFile(rel).exists(); }

    long cacheAge(String rel) {
        File f = cacheFile(rel);
        return f.exists() ? System.currentTimeMillis() - f.lastModified() : Long.MAX_VALUE;
    }

    String readCache(String rel) {
        try {
            File f = cacheFile(rel);
            if (!f.exists()) return null;
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return bo.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    void writeCache(String rel, String data) {
        try {
            File f = cacheFile(rel);
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            File tmp = new File(f.getPath() + "." + Thread.currentThread().getId() + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            out.write(data.getBytes("UTF-8"));
            out.close();
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f); }
        } catch (Exception ignored) { }
    }

    String httpGet(String urlStr) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent", "EDSmg/2.3 Android");
                c.setRequestProperty("Accept", "application/json");
                int code = c.getResponseCode();
                if (code != 200) {
                    mLastErr = "HTTP " + code;
                    try {
                        InputStream es = c.getErrorStream();
                        if (es != null) { byte[] b = new byte[4096]; while (es.read(b) > 0) { } es.close(); }
                    } catch (Exception ignored) { }
                    if (code >= 400 && code < 500) return null;
                    continue;
                }
                InputStream in = c.getInputStream();
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();   // averina any amin'ny pool ny fifandraisana (keep-alive) => haingana kokoa
                return bo.toString("UTF-8");
            } catch (Exception e) {
                mLastErr = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }
        return null;
    }

    /** JSON de l'API : cache si assez récent (ou hors ligne), sinon réseau puis cache. */
    String fetchApi(String rel, long maxAge) {
        String cached = readCache(rel);
        if (cached != null && (maxAge == FOREVER || !isOnline() || cacheAge(rel) <= maxAge)) return cached;
        if (!isOnline()) { mLastErr = "Tsy misy Internet"; return cached; }
        String r = httpGet(API + rel);
        if (r != null) { writeCache(rel, r); return r; }
        return cached;
    }

    // ── Images (cache disque) ──────────────────────────────────────────────
    File imgFile(String url) {
        String h;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(url.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            h = sb.toString();
        } catch (Exception e) {
            h = Integer.toHexString(url.hashCode());
        }
        return new File(new File(getFilesDir(), "lkl_img"), h);
    }

    boolean imgCached(String url) { return url == null || url.isEmpty() || imgFile(url).exists(); }

    boolean downloadImage(String url) {
        if (url == null || url.isEmpty()) return true;
        File f = imgFile(url);
        if (f.exists()) return true;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                File dir = f.getParentFile();
                if (dir != null) dir.mkdirs();
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(25000);
                c.setInstanceFollowRedirects(true);
                if (c.getResponseCode() != 200) continue;
                InputStream in = c.getInputStream();
                File tmp = new File(f.getPath() + "." + Thread.currentThread().getId() + ".tmp");
                FileOutputStream out = new FileOutputStream(tmp);
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.close();
                in.close();
                if (tmp.renameTo(f)) return true;
            } catch (Exception ignored) { }
        }
        return false;
    }

    Bitmap getBitmap(String url, int maxW) {
        try {
            File f = imgFile(url);
            if (!f.exists() && isOnline()) downloadImage(url, f);
            if (!f.exists()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            int ss = 1;
            while (o.outWidth / ss > maxW * 2) ss *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = ss;
            return BitmapFactory.decodeFile(f.getPath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    void downloadImage(String url, File f) { downloadImage(url); }

    /** Lesona Lehibe : kapaina ny lohateny ambony sy ny reliure havanana. */
    void loadHeroCrop(final ImageView iv, final String url) {
        if (url == null || url.isEmpty()) return;
        mImgPool.execute(new Runnable() {
            @Override public void run() {
                Bitmap c = getBitmap(url, 1080);
                if (c == null) return;
                Bitmap bm;
                try {
                    int w = c.getWidth(), h = c.getHeight();
                    bm = Bitmap.createBitmap(c, 0, (int) (h * 0.15f), (int) (w * 0.83f), (int) (h * 0.85f));
                } catch (Throwable t) {
                    bm = c;
                }
                final Bitmap fb = bm;
                mHandler.post(new Runnable() {
                    @Override public void run() { iv.setImageBitmap(fb); }
                });
            }
        });
    }

    void loadInto(final ImageView iv, final String url, final int maxW) {
        if (url == null || url.isEmpty()) return;
        mImgPool.execute(new Runnable() {
            @Override public void run() {
                final Bitmap bm = getBitmap(url, maxW);
                if (bm != null) mHandler.post(new Runnable() {
                    @Override public void run() { iv.setImageBitmap(bm); }
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // PARSING
    // ══════════════════════════════════════════════════════════════════════
    static final Pattern PAT_LEHIBE = Pattern.compile("^\\d{4}-\\d{2}$");
    static final Pattern PAT_TANORA = Pattern.compile("^\\d{4}-\\d{2}-cq$");
    static final Pattern PAT_ZATOVO = Pattern.compile("^\\d{4}-\\d{2}-cc$");

    boolean matchesSokajy(String id, int s) {
        if (s == 0) return PAT_LEHIBE.matcher(id).matches();
        if (s == 1) return PAT_TANORA.matcher(id).matches();
        if (s == 2) return PAT_ZATOVO.matcher(id).matches();
        return false;
    }

    List<Quarter> parseQuarters(String json, int sokajy) {
        List<Quarter> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.optString("id", "");
                if (!matchesSokajy(id, sokajy)) continue;
                Quarter q = new Quarter();
                q.id = id;
                q.title = o.optString("title", o.optString("quarterly_name", id));
                q.cover = o.optString("cover", o.optString("quarterly_cover", ""));
                q.human = o.optString("human_date", "");
                q.start = o.optString("start_date", "");
                q.end = o.optString("end_date", "");
                q.desc = o.optString("description", "");
                out.add(q);
            }
        } catch (Exception ignored) { }
        Collections.sort(out, (a, b) -> b.id.compareTo(a.id));
        return out;
    }

    List<Lesson> parseLessons(String qj) {
        List<Lesson> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONObject(qj).optJSONArray("lessons");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Lesson l = new Lesson();
                l.id = o.optString("id", String.format(Locale.US, "%02d", i + 1));
                l.title = o.optString("title", "");
                l.cover = o.optString("cover", "");
                l.start = o.optString("start_date", "");
                l.end = o.optString("end_date", "");
                out.add(l);
            }
        } catch (Exception ignored) { }
        return out;
    }

    Quarter pickCurrent(List<Quarter> qs) {
        if (qs.isEmpty()) return null;
        Date t = today0();
        for (Quarter q : qs) if (inRange(q.start, q.end, t)) return q;
        for (Quarter q : qs) {
            Date a = parseDate(q.start);
            if (a != null && !a.after(t)) return q;
        }
        Calendar c = Calendar.getInstance();
        String pre = c.get(Calendar.YEAR) + "-" + String.format(Locale.US, "%02d", c.get(Calendar.MONTH) / 3 + 1);
        for (Quarter q : qs) if (q.id.startsWith(pre)) return q;
        return qs.get(0);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PAGE D'ACCUEIL (telovolana + lisitry ny lesona)
    // ══════════════════════════════════════════════════════════════════════
    void buildHome(final String wantQuarter) {
        final int tok = ++mPageToken;
        mPage = 1;
        mReaderContent = null;
        mRoot.removeAllViews();
        mDrawer = null;
        final FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(C_BG);
        mRoot.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));

        final ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col, new FrameLayout.LayoutParams(MATCH, WRAP));

        // ── En-tête ──
        final FrameLayout hero = new FrameLayout(this);
        col.addView(hero, new LinearLayout.LayoutParams(MATCH, dp(450)));
        final ImageView heroImg = addHeroImage(hero);

        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setGravity(Gravity.CENTER_HORIZONTAL);
        ht.setPadding(dp(22), 0, dp(22), dp(18));
        hero.addView(ht, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));

        final TextView heroTitle = tv("", 26, 0xFFFFFFFF, true);
        heroTitle.setGravity(Gravity.CENTER);
        ht.addView(heroTitle, new LinearLayout.LayoutParams(MATCH, WRAP));
        final TextView heroDate = tv("", 16, 0xCCFFFFFF, false);
        heroDate.setGravity(Gravity.CENTER);
        ht.addView(heroDate, lpm(MATCH, WRAP, 0, dp(6), 0, 0));

        final TextView readBtn = tv("HAMAKY", 14, 0xFFFFFFFF, true);
        readBtn.setLetterSpacing(0.12f);
        readBtn.setGravity(Gravity.CENTER);
        readBtn.setBackgroundDrawable(rrs(0x66000000, dp(30), dp(1), 0x99FFFFFF));
        readBtn.setPadding(dp(40), dp(14), dp(40), dp(14));
        ht.addView(readBtn, lpm(WRAP, WRAP, 0, dp(18), 0, 0));

        final TextView heroDesc = tv("", 14, 0xE6FFFFFF, false);
        heroDesc.setGravity(Gravity.CENTER);
        heroDesc.setMaxLines(3);
        heroDesc.setEllipsize(TextUtils.TruncateAt.END);
        heroDesc.setLineSpacing(dp(2), 1f);
        ht.addView(heroDesc, lpm(MATCH, WRAP, 0, dp(16), 0, 0));
        final TextView more = tv("Hijery bebe kokoa", 14, 0xFFFFFFFF, false);
        more.setTypeface(null, Typeface.ITALIC);
        more.setVisibility(View.GONE);
        ht.addView(more, lpm(WRAP, WRAP, 0, dp(4), 0, 0));
        more.setOnClickListener(v -> {
            boolean expanded = heroDesc.getMaxLines() > 3;
            heroDesc.setMaxLines(expanded ? 3 : 100);
            more.setText(expanded ? "Hijery bebe kokoa" : "Hanafina");
        });

        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(12), dp(16), dp(40));
        col.addView(list, new LinearLayout.LayoutParams(MATCH, WRAP));
        ProgressBar pb = new ProgressBar(this);
        list.addView(pb, lpm(WRAP, WRAP, 0, dp(30), 0, 0));
        ((LinearLayout.LayoutParams) pb.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;

        // ── Barre du haut ──
        LinearLayout row = makeBar(page, scroll, 280);
        row.addView(iconBtn(Ico.MENU, v -> openDrawer()), new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackgroundDrawable(rr(0x66000000, dp(22)));
        pill.setPadding(dp(14), 0, dp(14), 0);
        pill.addView(new FlagView(this), new LinearLayout.LayoutParams(dp(24), dp(16)));
        pill.addView(tv("MG", 16, 0xFFFFFFFF, false), lpm(WRAP, WRAP, dp(8), 0, 0, 0));
        pill.setOnClickListener(v -> showQuarterSheet());
        row.addView(pill, new LinearLayout.LayoutParams(WRAP, dp(44)));
        row.addView(iconBtn(Ico.CAL, v -> showDatePicker()), lpm(dp(44), dp(44), dp(8), 0, 0, 0));
        row.addView(iconBtn(Ico.FILTER, v -> showQuarterSheet()), lpm(dp(44), dp(44), dp(8), 0, 0, 0));

        // ── Chargement ──
        new Thread(() -> {
            String idx = fetchApi("index.json", LIST_AGE);
            if (idx == null) {
                final String err = mLastErr;
                mHandler.post(() -> {
                    if (tok != mPageToken) return;
                    list.removeAllViews();
                    list.addView(errBox("Tsy vita ny fetching. Jereo ny Internet.\n(" + err + ")",
                            () -> buildHome(wantQuarter)));
                });
                return;
            }
            final List<Quarter> qs = parseQuarters(idx, mSokajy);
            Quarter pick = null;
            if (wantQuarter != null && !wantQuarter.isEmpty())
                for (Quarter q : qs) if (q.id.equals(wantQuarter)) pick = q;
            if (pick == null) pick = pickCurrent(qs);
            final Quarter cur = pick;
            if (cur == null) {
                mHandler.post(() -> {
                    if (tok != mPageToken) return;
                    list.removeAllViews();
                    list.addView(errBox("Tsy misy lesona hita.", () -> buildHome(null)));
                });
                return;
            }
            String qj = fetchApi(cur.id + "/index.json", LIST_AGE);
            final List<Lesson> lessons = qj == null ? new ArrayList<Lesson>() : parseLessons(qj);
            String d = cur.desc, cv = cur.cover;
            try {
                if (qj != null) {
                    JSONObject qo = new JSONObject(qj).optJSONObject("quarterly");
                    if (qo != null) {
                        String dd = qo.optString("description", "");
                        if (!dd.isEmpty()) d = dd;
                        String c2 = qo.optString("cover", "");
                        if (!c2.isEmpty()) cv = c2;
                    }
                }
            } catch (Exception ignored) { }
            final String desc = d, cover = cv;
            mQColorP = qj == null ? 0 : quarterColor(qj);
            mQuarters = qs;
            mLessons = lessons;
            mLessonsQid = cur.id;
            mHandler.post(() -> {
                if (tok != mPageToken) return;
                mQuarterId = cur.id;
                heroTitle.setText(cur.title);
                heroDate.setText(cur.human);
                if (matchesSokajy(cur.id, 0)) loadHeroCrop(heroImg, cover);
                else loadInto(heroImg, cover, 1080);
                String dtext = plain(desc);
                if (!dtext.isEmpty()) {
                    heroDesc.setText(dtext);
                    more.setVisibility(View.VISIBLE);
                }
                list.removeAllViews();
                if (lessons.isEmpty()) {
                    list.addView(errBox("Tsy azo ny lisitry ny lesona. Jereo ny Internet.\n(" + mLastErr + ")",
                            () -> buildHome(wantQuarter)));
                    return;
                }
                Date today = today0();
                int curIdx = -1;
                for (int i = 0; i < lessons.size(); i++)
                    if (inRange(lessons.get(i).start, lessons.get(i).end, today)) curIdx = i;
                final int openIdx = curIdx >= 0 ? curIdx : 0;
                final int openDay = curIdx >= 0 ? dayForToday(lessons.get(curIdx)) : 1;
                readBtn.setOnClickListener(v -> buildReader(cur.id, lessons.get(openIdx).id, openDay, 0));
                if (HAS_TSIANJERY[mSokajy]) list.addView(tsianjeryCard(cur, lessons));
                for (int i = 0; i < lessons.size(); i++) {
                    final Lesson ls = lessons.get(i);
                    final boolean isCur = i == curIdx;
                    final int day = isCur ? dayForToday(ls) : 1;
                    list.addView(lessonCard(cur, ls, i, isCur, day));
                }
            });
        }).start();
    }

    int parseColorSafe(String c) {
        try { return c == null || c.isEmpty() ? 0 : Color.parseColor(c.startsWith("#") ? c : "#" + c); }
        catch (Exception e) { return 0; }
    }

    int quarterColor(String qj) {
        try {
            JSONObject qo = new JSONObject(qj).optJSONObject("quarterly");
            if (qo != null) return parseColorSafe(qo.optString("color_primary", ""));
        } catch (Exception ignored) { }
        return 0;
    }

    int lighten(int c, float t) {
        int r = (int) (Color.red(c) + (255 - Color.red(c)) * t);
        int g = (int) (Color.green(c) + (255 - Color.green(c)) * t);
        int b = (int) (Color.blue(c) + (255 - Color.blue(c)) * t);
        return Color.rgb(r, g, b);
    }

    void applyTheme(boolean youth, int primary) {
        mYouth = youth;
        if (youth) {
            mThAccent = primary != 0 ? primary : 0xFFD81B60;
            mThText = 0xFF2A2230;
            mThLink = mThAccent;
            mThMuted = 0x99000000;
        } else {
            mThAccent = C_ACCENT;
            mThText = C_TEXT;
            mThLink = C_LINK;
            mThMuted = C_MUTED;
        }
    }

    int dayForToday(Lesson l) {
        Date a = parseDate(l.start);
        if (a == null) return 1;
        return (int) Math.max(1, Math.min(7, daysBetween(a, today0()) + 1));
    }

    View lessonCard(final Quarter q, final Lesson ls, int i, boolean isCur, final int day) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(12), dp(14));
        card.setBackgroundDrawable(isCur ? rrs(C_CARD, dp(18), dp(2), 0xFF6C7BFF)
                : rrs(C_CARD, dp(18), dp(1), C_LINE));
        LinearLayout.LayoutParams clp = lpm(MATCH, WRAP, 0, 0, 0, dp(14));
        card.setLayoutParams(clp);

        ImageView th = new ImageView(this);
        th.setScaleType(ImageView.ScaleType.CENTER_CROP);
        th.setBackgroundColor(0xFF2A2B38);
        roundImage(th, dp(10));
        card.addView(th, new LinearLayout.LayoutParams(dp(56), dp(56)));
        loadInto(th, ls.cover, 240);

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        card.addView(tx, lpm(0, WRAP, dp(14), 0, dp(8), 0));
        ((LinearLayout.LayoutParams) tx.getLayoutParams()).weight = 1f;

        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        r1.addView(tv("Lesona " + (i + 1), 16, C_ACCENT, true));
        if (isCur) {
            TextView badge = tv("LESONA AMIN'IZAO", 10, 0xFF000000, true);
            badge.setBackgroundDrawable(rr(0xFFFFFFFF, dp(12)));
            badge.setPadding(dp(10), dp(3), dp(10), dp(3));
            r1.addView(badge, lpm(WRAP, WRAP, dp(10), 0, 0, 0));
        }
        tx.addView(r1, new LinearLayout.LayoutParams(MATCH, WRAP));
        tx.addView(tv(ls.title, 17, C_TEXT, true), lpm(MATCH, WRAP, 0, dp(2), 0, 0));
        LinearLayout r3 = new LinearLayout(this);
        r3.setOrientation(LinearLayout.HORIZONTAL);
        r3.setGravity(Gravity.CENTER_VERTICAL);
        r3.addView(new Ico(this, Ico.CAL).color(C_MUTED), new LinearLayout.LayoutParams(dp(16), dp(16)));
        r3.addView(tv(ls.start + " - " + ls.end, 14, C_MUTED, false), lpm(WRAP, WRAP, dp(6), 0, 0, 0));
        tx.addView(r3, lpm(MATCH, WRAP, 0, dp(6), 0, 0));

        card.addView(new Ico(this, Ico.CHEV).color(C_ACCENT), new LinearLayout.LayoutParams(dp(20), dp(20)));
        card.setOnClickListener(v -> buildReader(q.id, ls.id, day, 0));
        return card;
    }

    View tsianjeryCard(final Quarter q, final List<Lesson> lessons) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));
        card.setBackgroundDrawable(rrs(mDark ? 0xFF262012 : 0xFFFFF6DD, dp(18), dp(1), 0x99D4A017));
        card.setLayoutParams(lpm(MATCH, WRAP, 0, 0, 0, dp(14)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        card.addView(tx, new LinearLayout.LayoutParams(0, WRAP, 1f));
        tx.addView(tv("✦  Tsianjery " + lessons.size(), 18, C_GOLD, true));
        tx.addView(tv("Ny lohateny sy ny tsianjery isaky ny herinandro", 13, C_MUTED, false),
                lpm(MATCH, WRAP, 0, dp(4), 0, 0));
        card.addView(new Ico(this, Ico.CHEV).color(C_GOLD), new LinearLayout.LayoutParams(dp(20), dp(20)));
        card.setOnClickListener(v -> buildTsianjery(q, lessons));
        return card;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FIFIDIANANA TELOVOLANA (bottom sheet)
    // ══════════════════════════════════════════════════════════════════════
    Dialog newSheet(boolean full) {
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    void showSheet(Dialog d, View content, int heightPx) {
        d.setContentView(content);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(MATCH, heightPx);
            w.setGravity(Gravity.BOTTOM);
        }
        d.show();
    }

    GradientDrawable topRounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return g;
    }

    void showQuarterSheet() {
        final Dialog dlg = newSheet(true);
        final SwipeSheet root = new SwipeSheet(this);
        root.setBackgroundDrawable(topRounded(C_SHEET, dp(26)));
        root.setPadding(0, dp(4), 0, 0);
        root.addView(handleBox(), new LinearLayout.LayoutParams(MATCH, dp(36)));
        root.onDismiss = dlg::dismiss;

        TextView title = tv("Misafidiana lesona", 22, C_TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        final int[] cur = {mSokajy};
        final TextView[] tabs = new TextView[3];
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout tabBox = new LinearLayout(this);
        tabBox.setOrientation(LinearLayout.HORIZONTAL);
        tabBox.setPadding(dp(5), dp(5), dp(5), dp(5));
        tabBox.setBackgroundDrawable(rrs(mDark ? 0xFF000000 : 0xFFEDEFF5, dp(30), dp(1), mDark ? 0xFFE0E0E8 : C_LINE));
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tabs[i] = tv(SOKAJY_NAMES[i], 16, C_TEXT, false);
            tabs[i].setGravity(Gravity.CENTER);
            tabs[i].setPadding(dp(20), dp(12), dp(20), dp(12));
            tabs[i].setOnClickListener(v -> { cur[0] = idx; refreshHolder(cur, tabs, list, dlg).run(); });
            tabBox.addView(tabs[i]);
        }
        hs.addView(tabBox);
        LinearLayout.LayoutParams hlp = lpm(MATCH, WRAP, dp(14), dp(20), dp(14), dp(8));
        root.addView(hs, hlp);

        ScrollView sv = new ScrollView(this);
        sv.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        root.scroller = sv;

        refreshHolder(cur, tabs, list, dlg).run();
        int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.82f);
        showSheet(dlg, root, h);
    }

    /** Rafraîchit les onglets + la liste de la feuille. */
    Runnable refreshHolder(final int[] cur, final TextView[] tabs, final LinearLayout list, final Dialog dlg) {
        return new Runnable() {
            @Override public void run() {
                for (int i = 0; i < 3; i++) {
                    boolean sel = i == cur[0];
                    tabs[i].setBackgroundDrawable(sel ? rr(mDark ? 0xFFFFFFFF : 0xFF1B1C25, dp(24)) : null);
                    tabs[i].setTextColor(sel ? (mDark ? 0xFF000000 : 0xFFFFFFFF) : C_TEXT);
                    tabs[i].setTypeface(null, sel ? Typeface.BOLD : Typeface.NORMAL);
                }
                fillSheetList(list, cur[0], dlg);
            }
        };
    }

    void fillSheetList(final LinearLayout list, final int sok, final Dialog dlg) {
        list.removeAllViews();
        ProgressBar pb = new ProgressBar(this);
        list.addView(pb, lpm(WRAP, WRAP, 0, dp(30), 0, 0));
        ((LinearLayout.LayoutParams) pb.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        new Thread(() -> {
            String idx = fetchApi("index.json", LIST_AGE);
            final List<Quarter> qs = idx == null ? new ArrayList<Quarter>() : parseQuarters(idx, sok);
            mHandler.post(() -> {
                list.removeAllViews();
                if (qs.isEmpty()) {
                    list.addView(errBox("Tsy misy lesona hita.", null));
                    return;
                }
                for (final Quarter q : qs) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(18), dp(12), dp(18), dp(12));
                    ImageView iv = new ImageView(this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setBackgroundColor(0xFF2A2B38);
                    roundImage(iv, dp(6));
                    row.addView(iv, new LinearLayout.LayoutParams(dp(64), dp(86)));
                    loadInto(iv, q.cover, 300);
                    LinearLayout tx = new LinearLayout(this);
                    tx.setOrientation(LinearLayout.VERTICAL);
                    row.addView(tx, lpm(0, WRAP, dp(16), 0, 0, 0));
                    ((LinearLayout.LayoutParams) tx.getLayoutParams()).weight = 1f;
                    tx.addView(tv(q.title, 18, C_TEXT, true));
                    tx.addView(tv(q.human, 15, C_MUTED, false), lpm(MATCH, WRAP, 0, dp(4), 0, 0));
                    row.setOnClickListener(v -> {
                        dlg.dismiss();
                        mSokajy = sok;
                        mPrefs.edit().putInt("sokajy", sok).apply();
                        buildHome(q.id);
                    });
                    list.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
                    View div = new View(this);
                    div.setBackgroundColor(C_LINE);
                    list.addView(div, lpm(MATCH, dp(1), dp(96), 0, 0, 0));
                }
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // MENU / INFO / DATE
    // ══════════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════
    // MENU TIROIR (drawer havia)
    // ══════════════════════════════════════════════════════════════════════
    FrameLayout mDrawer;
    static final String[] DRAWER_NAMES = {"Lehibe", "Tanora Zokiny", "Zatovo"};

    View drawerLabel(String t) {
        TextView v = tv(t, 12, C_MUTED, true);
        v.setLetterSpacing(0.12f);
        v.setPadding(dp(28), dp(18), dp(20), dp(6));
        return v;
    }

    View drawerItem(int ico, String title, String sub, boolean sel, View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(14), dp(12));
        if (sel) row.setBackgroundDrawable(rr((C_ACCENT & 0x00FFFFFF) | 0x2E000000, dp(16)));
        row.addView(new Ico(this, ico).color(sel ? C_ACCENT : C_TEXT), new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(tv(title, 16, sel ? C_ACCENT : C_TEXT, sel));
        if (sub != null) tx.addView(tv(sub, 12, C_MUTED, false), lpm(MATCH, WRAP, 0, dp(2), 0, 0));
        row.addView(tx, new LinearLayout.LayoutParams(0, WRAP, 1f));
        ((LinearLayout.LayoutParams) tx.getLayoutParams()).leftMargin = dp(16);
        row.setOnClickListener(l);
        row.setLayoutParams(lpm(MATCH, WRAP, dp(12), dp(2), dp(12), dp(2)));
        return row;
    }

    String lastSyncText() {
        long t = mPrefs.getLong("last_sync", 0);
        if (t == 0) return "Mbola tsy nisy";
        return "Farany: " + new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(new Date(t));
    }

    void openDrawer() {
        if (mDrawer != null) return;
        final FrameLayout overlay = new FrameLayout(this);
        final View scrim = new View(this);
        scrim.setBackgroundColor(0x99000000);
        scrim.setAlpha(0f);
        scrim.setOnClickListener(v -> closeDrawer());
        overlay.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));

        int w = Math.min(dp(330), (int) (getResources().getDisplayMetrics().widthPixels * 0.84f));
        final LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(mDark ? 0xFF111218 : 0xFFFFFFFF);
        pbg.setCornerRadii(new float[]{0, 0, dp(26), dp(26), dp(26), dp(26), 0, 0});
        panel.setBackgroundDrawable(pbg);
        panel.setClickable(true);
        panel.setPadding(0, statusH(), 0, 0);
        overlay.addView(panel, new FrameLayout.LayoutParams(w, MATCH));

        // ── en-tête : EDS mg ──
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(24), dp(22), dp(20), dp(10));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        roundImage(logo, dp(12));
        head.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        TextView title = tv("EDS mg", 28, 0xFFFFFFFF, true);
        title.getPaint().setShader(new LinearGradient(0, 0, dp(110), 0,
                new int[]{0xFF5BC0F8, 0xFFB86CFF, 0xFFE8456B}, null, Shader.TileMode.CLAMP));
        ht.addView(title);
        ht.addView(tv(fmtDate(today0()), 13, C_MUTED, false), lpm(MATCH, WRAP, 0, dp(2), 0, 0));
        head.addView(ht, lpm(0, WRAP, dp(14), 0, 0, 0));
        ((LinearLayout.LayoutParams) ht.getLayoutParams()).weight = 1f;
        panel.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        sv.addView(items, new FrameLayout.LayoutParams(MATCH, WRAP));
        panel.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        items.addView(drawerLabel("LESONA"));
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            items.addView(drawerItem(Ico.BOOK, DRAWER_NAMES[i], null, i == mSokajy, v -> {
                closeDrawer();
                if (idx != mSokajy || mPage != 1) {
                    mSokajy = idx;
                    mPrefs.edit().putInt("sokajy", idx).apply();
                    buildHome(null);
                }
            }));
        }
        View div = new View(this);
        div.setBackgroundColor(C_LINE);
        items.addView(div, lpm(MATCH, dp(1), dp(24), dp(14), dp(24), dp(2)));
        items.addView(drawerLabel("KIRAKIRA"));
        items.addView(drawerItem(Ico.REFRESH, "Fanavaozana", lastSyncText(), false, v -> {
            closeDrawer();
            startAutoSync(true);
        }));
        items.addView(drawerItem(Ico.AA, "Haben'ny soratra", mTextSp + " sp", false, v -> {
            closeDrawer();
            showTextSizeDialog();
        }));
        items.addView(drawerItem(mDark ? Ico.MOON : Ico.SUN, "Efijery", mDark ? "Matroka (dark)" : "Mazava (claire)", false, null));
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setBackgroundDrawable(rr(mDark ? 0xFF1B1C25 : 0xFFEDEFF5, dp(16)));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int k = 0; k < 2; k++) {
            final boolean dk = k == 0;
            boolean on = dk == mDark;
            TextView b = tv(dk ? "Matroka" : "Mazava", 14, on ? 0xFFFFFFFF : C_MUTED, on);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, dp(10), 0, dp(10));
            if (on) b.setBackgroundDrawable(rr(C_ACCENT, dp(12)));
            b.setOnClickListener(v -> { if (dk != mDark) setAppTheme(dk); });
            seg.addView(b, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        items.addView(seg, lpm(MATCH, WRAP, dp(28), dp(2), dp(28), dp(10)));

        // ── Momba ny app (en couleur) ──
        LinearLayout about = new LinearLayout(this);
        about.setOrientation(LinearLayout.HORIZONTAL);
        about.setGravity(Gravity.CENTER_VERTICAL);
        about.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable ag = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF4F46E5, 0xFFB44CF2, 0xFFE8456B});
        ag.setCornerRadius(dp(20));
        about.setBackgroundDrawable(ag);
        about.addView(new Ico(this, Ico.INFO), new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout at = new LinearLayout(this);
        at.setOrientation(LinearLayout.VERTICAL);
        at.addView(tv("Momba ny app", 16, 0xFFFFFFFF, true));
        at.addView(tv("EDS mg · v2.3", 12, 0xCCFFFFFF, false), lpm(MATCH, WRAP, 0, dp(2), 0, 0));
        about.addView(at, lpm(0, WRAP, dp(14), 0, 0, 0));
        ((LinearLayout.LayoutParams) at.getLayoutParams()).weight = 1f;
        about.addView(new Ico(this, Ico.CHEV), new LinearLayout.LayoutParams(dp(20), dp(20)));
        about.setOnClickListener(v -> { closeDrawer(); showInfo(); });
        panel.addView(about, lpm(MATCH, WRAP, dp(16), dp(8), dp(16), dp(18)));

        panel.setTranslationX(-w);
        mRoot.addView(overlay, new FrameLayout.LayoutParams(MATCH, MATCH));
        mDrawer = overlay;
        panel.animate().translationX(0f).setDuration(230).start();
        scrim.animate().alpha(1f).setDuration(230).start();
    }

    void closeDrawer() {
        if (mDrawer == null) return;
        final FrameLayout ov = mDrawer;
        mDrawer = null;
        View scrim = ov.getChildAt(0), panel = ov.getChildAt(1);
        scrim.animate().alpha(0f).setDuration(200).start();
        panel.animate().translationX(-panel.getWidth()).setDuration(200).withEndAction(new Runnable() {
            @Override public void run() { mRoot.removeView(ov); }
        }).start();
    }

    void setAppTheme(boolean dark) {
        mPrefs.edit().putBoolean("dark", dark).apply();
        applyAppTheme(dark);
        mDrawer = null;
        if (mPage == 3) buildReader(mQuarterId, mLessonId, mDayNum, 0);
        else buildHome(mQuarterId);
    }

    void showInfo() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundDrawable(rr(C_SHEET, dp(26)));

        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setGravity(Gravity.CENTER_HORIZONTAL);
        banner.setPadding(dp(24), dp(26), dp(24), dp(22));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4F46E5, 0xFFB44CF2, 0xFFE8456B});
        g.setCornerRadii(new float[]{dp(26), dp(26), dp(26), dp(26), 0, 0, 0, 0});
        banner.setBackgroundDrawable(g);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        roundImage(logo, dp(16));
        banner.addView(logo, new LinearLayout.LayoutParams(dp(68), dp(68)));
        TextView name = tv("EDS mg", 26, 0xFFFFFFFF, true);
        banner.addView(name, lpm(WRAP, WRAP, 0, dp(12), 0, 0));
        banner.addView(tv("Lesona Kilasy Lehibe", 14, 0xE6FFFFFF, false), lpm(WRAP, WRAP, 0, dp(2), 0, 0));
        banner.addView(tv("Dikan-teny 2.3", 12, 0xB3FFFFFF, false), lpm(WRAP, WRAP, 0, dp(4), 0, 0));
        box.addView(banner, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView msg = tv("Lesona isan'andro amin'ny teny malagasy, azo vakiana offline aorian'ny fanavaozana "
                + "voalohany.\n\nBaiboly: malagasy 1865.\nNovolavolain'i Sitraka Nambinintsoa.", 14, C_TEXT, false);
        msg.setLineSpacing(dp(3), 1f);
        box.addView(msg, lpm(MATCH, WRAP, dp(24), dp(20), dp(24), 0));
        TextView ok = tv("OK", 15, 0xFFFFFFFF, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackgroundDrawable(rr(C_ACCENT, dp(24)));
        ok.setPadding(dp(36), dp(11), dp(36), dp(11));
        ok.setOnClickListener(v -> d.dismiss());
        box.addView(ok, lpm(WRAP, WRAP, dp(24), dp(18), dp(24), dp(20)));
        ((LinearLayout.LayoutParams) ok.getLayoutParams()).gravity = Gravity.END;
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(Math.min(dp(340), (int) (getResources().getDisplayMetrics().widthPixels * 0.9f)), WRAP);
        }
        d.show();
    }

    void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(this,
                mDark ? android.R.style.Theme_Material_Dialog : android.R.style.Theme_Material_Light_Dialog, (view, y, m, d) -> {
            Calendar x = Calendar.getInstance();
            x.set(y, m, d, 0, 0, 0);
            x.set(Calendar.MILLISECOND, 0);
            openByDate(x.getTime());
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.setButton(DialogInterface.BUTTON_NEUTRAL, "Anio", (di, w) -> openByDate(today0()));
        dlg.show();
    }

    /** Ouvre la leçon/le jour correspondant à une date (dans la catégorie courante). */
    void openByDate(final Date date) {
        final int sok = mSokajy;
        toast("Mikaroka ny lesona amin'io daty io…");
        new Thread(() -> {
            String idx = fetchApi("index.json", LIST_AGE);
            List<Quarter> qs = idx == null ? new ArrayList<Quarter>() : parseQuarters(idx, sok);
            List<Quarter> cand = new ArrayList<>();
            for (Quarter q : qs) if (inRange(q.start, q.end, date)) cand.add(q);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            int y = c.get(Calendar.YEAR);
            for (Quarter q : qs) {
                if (cand.contains(q) || q.id.length() < 4) continue;
                try {
                    if (Math.abs(Integer.parseInt(q.id.substring(0, 4)) - y) <= 1) cand.add(q);
                } catch (Exception ignored) { }
            }
            String foundQ = null, foundL = null;
            int dayN = 1;
            for (Quarter q : cand) {
                String qj = fetchApi(q.id + "/index.json", LIST_AGE);
                if (qj == null) continue;
                for (Lesson l : parseLessons(qj)) {
                    if (inRange(l.start, l.end, date)) {
                        foundQ = q.id;
                        foundL = l.id;
                        dayN = (int) Math.max(1, Math.min(7, daysBetween(parseDate(l.start), date) + 1));
                        break;
                    }
                }
                if (foundQ != null) break;
            }
            final String fq = foundQ, fl = foundL;
            final int fd = dayN;
            mHandler.post(() -> {
                if (fq == null) toast("Tsy misy lesona amin'io daty io");
                else buildReader(fq, fl, fd, 0);
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PAGE DE LECTURE (header sary + lohateny + daty, swipe, note)
    // ══════════════════════════════════════════════════════════════════════
    void buildReader(final String qid, final String lid, final int dayNum, final int dir) {
        final int tok = ++mPageToken;
        mPage = 3;
        mQuarterId = qid;
        mLessonId = lid;
        mDayNum = dayNum;
        mDayHtml = null;
        applyTheme(qid.endsWith("-cc"), qid.equals(mLessonsQid) ? mQColorP : 0);
        final int youthTop = lighten(mThAccent, 0.62f);
        mRoot.removeAllViews();
        mDrawer = null;
        final FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(C_BG);
        mRoot.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));

        final ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col, new FrameLayout.LayoutParams(MATCH, WRAP));

        FrameLayout hero = new FrameLayout(this);
        col.addView(hero, new LinearLayout.LayoutParams(MATCH, dp(330)));
        final ImageView heroImg = addHeroImage(hero, mYouth ? youthTop : (mDark ? C_BG : DARK_SCRIM));
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setGravity(Gravity.CENTER_HORIZONTAL);
        ht.setPadding(dp(24), 0, dp(24), dp(34));
        hero.addView(ht, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));
        final TextView heroTitle = tv("", 26, 0xFFFFFFFF, true);
        heroTitle.setGravity(Gravity.CENTER);
        ht.addView(heroTitle, new LinearLayout.LayoutParams(MATCH, WRAP));
        final TextView heroDate = tv("", 17, 0xB3FFFFFF, false);
        heroDate.setGravity(Gravity.CENTER);
        ht.addView(heroDate, lpm(MATCH, WRAP, 0, dp(8), 0, 0));

        mReaderContent = new LinearLayout(this);
        mReaderContent.setOrientation(LinearLayout.VERTICAL);
        mReaderContent.setPadding(dp(20), dp(8), dp(20), dp(90));
        if (mYouth) mReaderContent.setBackgroundDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{youthTop, 0xFFFDE3B8}));
        col.addView(mReaderContent, new LinearLayout.LayoutParams(MATCH, WRAP));
        final LinearLayout content = mReaderContent;
        ProgressBar pb = new ProgressBar(this);
        content.addView(pb, lpm(WRAP, WRAP, 0, dp(30), 0, 0));
        ((LinearLayout.LayoutParams) pb.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;

        // barre du haut : retour | titre (apparaît en défilant) | date | Aa
        LinearLayout row = makeBar(page, scroll, 240);
        row.addView(iconBtn(Ico.BACK, v -> buildHome(mQuarterId)), new LinearLayout.LayoutParams(dp(44), dp(44)));
        final TextView barTitle = tv("", 17, 0xFFFFFFFF, true);
        barTitle.setTag("bartitle");
        barTitle.setSingleLine(true);
        barTitle.setEllipsize(TextUtils.TruncateAt.END);
        barTitle.setAlpha(0f);
        row.addView(barTitle, lpm(0, WRAP, dp(14), 0, dp(8), 0));
        ((LinearLayout.LayoutParams) barTitle.getLayoutParams()).weight = 1f;
        row.addView(iconBtn(Ico.CAL, v -> showDatePicker()), lpm(dp(44), dp(44), 0, 0, dp(8), 0));
        row.addView(iconBtn(Ico.AA, v -> showTextSizeDialog()), new LinearLayout.LayoutParams(dp(44), dp(44)));

        // points de position (swipe)
        mDotsRow = new LinearLayout(this);
        mDotsRow.setOrientation(LinearLayout.HORIZONTAL);
        mDotsRow.setGravity(Gravity.CENTER);
        mDotsRow.setBackgroundDrawable(rr(0x80000000, dp(12)));
        mDotsRow.setPadding(dp(8), dp(6), dp(8), dp(6));
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dlp.bottomMargin = dp(14);
        page.addView(mDotsRow, dlp);

        if (dir != 0) {
            page.setTranslationX(dir * dp(120));
            page.setAlpha(0f);
            page.animate().translationX(0).alpha(1f).setDuration(190).start();
        }

        final boolean[] coverLoaded = {false};
        if (qid.equals(mLessonsQid)) {
            for (Lesson l : mLessons) {
                if (l.id.equals(lid) && !l.cover.isEmpty()) {
                    loadInto(heroImg, l.cover, 1080);
                    coverLoaded[0] = true;
                }
            }
        }

        new Thread(() -> {
            if (!qid.equals(mLessonsQid)) {
                String qj = fetchApi(qid + "/index.json", LIST_AGE);
                if (qj != null) { mLessons = parseLessons(qj); mLessonsQid = qid; mQColorP = quarterColor(qj); }
            }
            String dkey = qid + "/" + lid;
            List<Day> days;
            String lcover;
            if (dkey.equals(mDaysKey)) {
                days = mDays;
                lcover = mLessonCover;
            } else {
                String lj = fetchApi(qid + "/lessons/" + lid + "/index.json", LIST_AGE);
                List<Day> nd = new ArrayList<>();
                String cv = "";
                if (lj != null) {
                    try {
                        JSONObject root = new JSONObject(lj);
                        JSONObject lo = root.optJSONObject("lesson");
                        if (lo != null) cv = lo.optString("cover", "");
                        JSONArray da = root.optJSONArray("days");
                        if (da != null) {
                            for (int i = 0; i < da.length(); i++) {
                                JSONObject o = da.getJSONObject(i);
                                Day d = new Day();
                                d.id = o.optString("id", String.format(Locale.US, "%02d", i + 1));
                                d.title = o.optString("title", "");
                                d.date = o.optString("date", "");
                                nd.add(d);
                            }
                        }
                    } catch (Exception ignored) { }
                }
                if (nd.isEmpty()) {
                    Date start = null;
                    for (Lesson l : mLessons) if (l.id.equals(lid)) start = parseDate(l.start);
                    for (int i = 0; i < 7; i++) {
                        Day d = new Day();
                        d.id = String.format(Locale.US, "%02d", i + 1);
                        if (start != null) {
                            Calendar c = Calendar.getInstance();
                            c.setTime(start);
                            c.add(Calendar.DAY_OF_YEAR, i);
                            d.date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(c.getTime());
                        }
                        nd.add(d);
                    }
                } else {
                    mDays = nd;
                    mDaysKey = dkey;
                    mLessonCover = cv;
                }
                days = nd;
                lcover = cv;
            }
            final int n = days.size();
            final int dn = Math.max(1, Math.min(dayNum, n));
            final Day dd = days.get(dn - 1);
            String cj = fetchApi(qid + "/lessons/" + lid + "/days/" + dd.id + "/read/index.json", FOREVER);
            String html = null, title = dd.title, date = dd.date;
            if (cj != null) {
                try {
                    JSONObject o = new JSONObject(cj);
                    html = o.optString("content", "");
                    if (title.isEmpty()) title = o.optString("title", "");
                    if (date.isEmpty()) date = o.optString("date", "");
                } catch (Exception ignored) { }
            }
            final String fhtml = html, ftitle = title, fdate = date, fcover = lcover;
            mHandler.post(() -> {
                if (tok != mPageToken) return;
                mDayNum = dn;
                mDayKey = qid + "/" + lid + "/" + dd.id;
                mDayHtml = fhtml;
                heroTitle.setText(ftitle.toUpperCase(Locale.getDefault()));
                barTitle.setText(ftitle);
                heroDate.setText(fmtDate(parseDate(fdate)));
                if (!coverLoaded[0]) loadInto(heroImg, fcover, 1080);
                updateDots(n, dn);
                content.removeAllViews();
                if (fhtml == null) {
                    content.addView(errBox("Tsy azo ny lesona. Jereo ny Internet.\n(" + mLastErr + ")",
                            () -> buildReader(qid, lid, dn, 0)));
                } else {
                    fillContent(content, fhtml, mDayKey);
                }
            });
        }).start();
    }

    void updateDots(int n, int cur) {
        if (mDotsRow == null) return;
        mDotsRow.removeAllViews();
        for (int i = 1; i <= n; i++) {
            View dot = new View(this);
            dot.setBackgroundDrawable(rr(i == cur ? C_ACCENT : 0x66FFFFFF, dp(3)));
            mDotsRow.addView(dot, lpm(i == cur ? dp(16) : dp(6), dp(6), dp(3), 0, dp(3), 0));
        }
    }

    void swipeDay(int dir) {
        if (mPage != 3) return;
        int n = mDays.size();
        int target = mDayNum + dir;
        if (n > 0 && target >= 1 && target <= n) { buildReader(mQuarterId, mLessonId, target, dir); return; }
        List<Lesson> ls = mLessons;
        int idx = -1;
        for (int i = 0; i < ls.size(); i++) if (ls.get(i).id.equals(mLessonId)) idx = i;
        if (idx < 0) return;
        int ni = idx + dir;
        if (ni < 0 || ni >= ls.size()) {
            toast(dir > 0 ? "Farany ity telovolana ity" : "Voalohany ity telovolana ity");
            return;
        }
        buildReader(mQuarterId, ls.get(ni).id, dir > 0 ? 1 : 7, dir);
    }

    void renderCurrentDay() {
        if (mPage == 3 && mReaderContent != null && mDayHtml != null) {
            mReaderContent.removeAllViews();
            fillContent(mReaderContent, mDayHtml, mDayKey);
        }
    }

    // ── Habe ny soratra (SeekBar) ──────────────────────────────────────────
    void showTextSizeDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundDrawable(rr(C_SHEET, dp(26)));
        box.setPadding(dp(24), dp(22), dp(24), dp(18));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(tv("Haben'ny soratra", 19, C_TEXT, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        final TextView val = tv(mTextSp + " sp", 14, 0xFFFFFFFF, true);
        val.setBackgroundDrawable(rr(C_ACCENT, dp(14)));
        val.setPadding(dp(12), dp(5), dp(12), dp(5));
        top.addView(val);
        box.addView(top, new LinearLayout.LayoutParams(MATCH, WRAP));

        final TextView sample = tv("Ohatra amin'ny haben'ny soratra hoentinao hamaky ny lesona.", mTextSp, C_TEXT, false);
        sample.setLineSpacing(dp(4), 1f);
        box.addView(sample, lpm(MATCH, WRAP, 0, dp(18), 0, dp(10)));

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(tv("A", 14, C_MUTED, true));
        final SeekBar sb = new SeekBar(this);
        sb.setMax(18);
        sb.setProgress(mTextSp - 14);
        try {
            sb.getThumb().setColorFilter(C_ACCENT, android.graphics.PorterDuff.Mode.SRC_IN);
            Drawable pd = sb.getProgressDrawable();
            if (pd instanceof LayerDrawable) {
                LayerDrawable ld = (LayerDrawable) pd;
                Drawable pr = ld.findDrawableByLayerId(android.R.id.progress);
                if (pr != null) pr.setColorFilter(C_ACCENT, android.graphics.PorterDuff.Mode.SRC_IN);
                Drawable bg = ld.findDrawableByLayerId(android.R.id.background);
                if (bg != null) bg.setColorFilter(C_LINE, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } catch (Exception ignored) { }
        row.addView(sb, new LinearLayout.LayoutParams(0, WRAP, 1f));
        row.addView(tv("A", 26, C_MUTED, true));
        box.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                mTextSp = 14 + progress;
                val.setText(mTextSp + " sp");
                sample.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSp);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                mPrefs.edit().putInt("tsize", mTextSp).apply();
                renderCurrentDay();
            }
        });

        TextView ok = tv("OK", 15, 0xFFFFFFFF, true);
        ok.setGravity(Gravity.CENTER);
        ok.setBackgroundDrawable(rr(C_ACCENT, dp(24)));
        ok.setPadding(dp(34), dp(10), dp(34), dp(10));
        ok.setOnClickListener(v -> d.dismiss());
        box.addView(ok, lpm(WRAP, WRAP, 0, dp(14), 0, 0));
        ((LinearLayout.LayoutParams) ok.getLayoutParams()).gravity = Gravity.END;
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(Math.min(dp(340), (int) (getResources().getDisplayMetrics().widthPixels * 0.9f)), WRAP);
        }
        d.show();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONTENU HTML (paragraphes, questions + notes, versets cliquables)
    // ══════════════════════════════════════════════════════════════════════
    @SuppressWarnings("deprecation")
    Spanned fromHtml(String h) {
        return Build.VERSION.SDK_INT >= 24 ? Html.fromHtml(h, Html.FROM_HTML_MODE_COMPACT) : Html.fromHtml(h);
    }

    String plain(String h) {
        if (h == null) return "";
        return fromHtml(h.replaceAll("(?is)<img[^>]*>", "")).toString().replace('\u00A0', ' ')
                .replace("\uFFFC", "").trim();
    }

    class VerseSpan extends ClickableSpan {
        final String ref;
        final int color;
        VerseSpan(String r, int c) { ref = r; color = c; }
        @Override public void onClick(View v) { showBiblePopup(ref); }
        @Override public void updateDrawState(TextPaint ds) {
            ds.setColor(color);
            ds.setUnderlineText(true);
        }
    }

    int indexOfChar(CharSequence cs, char ch, int from) {
        for (int i = from; i < cs.length(); i++) if (cs.charAt(i) == ch) return i;
        return -1;
    }

    /** HTML -> texte stylé ; les <a class="verse"> deviennent cliquables. */
    SpannableStringBuilder rich(String inner, int linkColor) {
        Matcher am = VERSE_A.matcher(inner);
        StringBuffer sb = new StringBuffer();
        while (am.find()) am.appendReplacement(sb, Matcher.quoteReplacement("\uE000" + am.group(1) + "\uE001"));
        am.appendTail(sb);
        SpannableStringBuilder out = new SpannableStringBuilder(fromHtml(sb.toString().replaceAll("(?is)<img[^>]*>", "")));
        for (URLSpan u : out.getSpans(0, out.length(), URLSpan.class)) out.removeSpan(u);
        for (TypefaceSpan t : out.getSpans(0, out.length(), TypefaceSpan.class)) out.removeSpan(t);
        List<int[]> rs = new ArrayList<>();
        int i = 0;
        while (true) {
            int s = indexOfChar(out, '\uE000', i);
            if (s < 0) break;
            int e = indexOfChar(out, '\uE001', s);
            if (e < 0) break;
            rs.add(new int[]{s, e});
            i = e + 1;
        }
        for (int k = rs.size() - 1; k >= 0; k--) {
            int s = rs.get(k)[0], e = rs.get(k)[1];
            String ref = out.subSequence(s + 1, e).toString().replace('\u00A0', ' ').trim();
            out.delete(e, e + 1);
            out.delete(s, s + 1);
            if (e - 1 > s && !ref.isEmpty())
                out.setSpan(new VerseSpan(ref, linkColor), s, e - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // nettoyage des espaces autour
        while (out.length() > 0 && Character.isWhitespace(out.charAt(out.length() - 1))) out.delete(out.length() - 1, out.length());
        while (out.length() > 0 && Character.isWhitespace(out.charAt(0))) out.delete(0, 1);
        return out;
    }

    /** Référence "Jao. 3:16" non balisée -> cliquable. */
    void autoLink(SpannableStringBuilder sb, int color) {
        Matcher m = BIBLE_PAT.matcher(sb.toString());
        while (m.find()) {
            if (sb.getSpans(m.start(1), m.end(1), VerseSpan.class).length > 0) continue;
            if (!BibleDb.isRef(m.group(1))) continue;
            sb.setSpan(new VerseSpan(m.group(1), color), m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** "Velomin'ny Teniny : Sal. 82" -> toko manontolo cliquable. */
    void velominFallback(SpannableStringBuilder sb) {
        String s = sb.toString();
        if (!s.toLowerCase(Locale.ROOT).startsWith("velomin")) return;
        if (sb.getSpans(0, sb.length(), VerseSpan.class).length > 0) return;
        int idx = s.indexOf(':');
        if (idx < 0) return;
        int a = idx + 1;
        while (a < s.length() && Character.isWhitespace(s.charAt(a))) a++;
        int e = s.length();
        while (e > a && Character.isWhitespace(s.charAt(e - 1))) e--;
        if (e > a && BibleDb.isRef(s.substring(a, e)))
            sb.setSpan(new VerseSpan(s.substring(a, e), mThLink), a, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static class Blk { String tag = "", attrs = "", inner = "", pl = ""; }

    List<Blk> parseBlocks(String html) {
        html = html.replaceAll("(?is)<script.*?</script>", "").replaceAll("(?is)<style.*?</style>", "")
                .replaceAll("(?is)<img[^>]*>", "");
        List<Blk> out = new ArrayList<>();
        Matcher m = BLOCK.matcher(html);
        while (m.find()) {
            Blk b = new Blk();
            b.tag = m.group(1).toLowerCase(Locale.ROOT);
            b.attrs = m.group(2) == null ? "" : m.group(2).toLowerCase(Locale.ROOT);
            b.inner = m.group(3);
            b.pl = plain(b.inner);
            if (!b.pl.isEmpty()) out.add(b);
        }
        if (out.isEmpty() && !plain(html).isEmpty()) {
            Blk b = new Blk();
            b.tag = "p";
            b.inner = html;
            b.pl = plain(html);
            out.add(b);
        }
        return out;
    }

    /** Référence seule sur une ligne (ex. "Salamo 146:4") ? */
    boolean isBareRef(String pl) {
        if (pl == null || pl.length() > 60) return false;
        String[] parts = pl.split(";");
        for (String x : parts) if (!BibleDb.isRef(x.trim())) return false;
        return pl.matches(".*\\d.*");
    }

    String dbText(String ref) {
        List<String[]> res = BibleDb.lookupAll(this, ref);
        StringBuilder sb = new StringBuilder();
        for (String[] r : res) {
            if (r[1] == null || r[1].isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(r[1].replaceAll("(?m)^\\d+\\s+", "").replace('\n', ' '));
        }
        return sb.toString().trim();
    }

    static String letters(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /** {andininy, référence} ; raha tsy misy texte dia alaina avy amin'ny Baiboly. */
    String[] splitVerseRef(String all) {
        all = all.trim();
        if (all.isEmpty()) return null;
        String ref = "";
        Matcher rm = REF_FULL.matcher(all);
        int rs = -1, re = -1;
        while (rm.find()) { rs = rm.start(); re = rm.end(); ref = rm.group(); }
        String verse = all;
        if (rs >= 0) verse = (all.substring(0, rs) + " " + all.substring(re)).trim();
        verse = verse.replaceAll("^[\\s\\-–—(\\[“\"«]+", "").replaceAll("[\\s\\-–—(\\[,;:”\"»)\\]]+$", "").trim();
        if (verse.length() < 8 && !ref.isEmpty()) {
            String t = dbText(ref.trim());
            if (!t.isEmpty()) verse = t;
        }
        if (verse.isEmpty() && ref.isEmpty()) return null;
        return new String[]{verse, ref.trim()};
    }

    /** Manomboka amin'ny blok "Tsianjery" : mamoaka {andininy, référence} ary ny farany. */
    String[] scanTsianjery(List<Blk> bl, int start, int[] end) {
        String t0 = bl.get(start).pl;
        int off = Math.min(9, t0.length());
        String first = t0.substring(off).replaceFirst("^[\\s:\\-–—]+", "");
        StringBuilder sb = new StringBuilder(first);
        int used = first.isEmpty() ? 0 : 1;
        if (!first.isEmpty() && REF_FULL.matcher(first).find()) used = 99;
        int j = start + 1;
        while (j < bl.size() && used < 3) {
            Blk b = bl.get(j);
            String lt = b.pl.toLowerCase(Locale.ROOT);
            if (b.tag.startsWith("h") || lt.startsWith("velomin") || lt.startsWith("fiambenana")
                    || lt.startsWith("tsianjery") || lt.startsWith("tantara fototra")) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(b.pl);
            used++;
            j++;
            if (REF_FULL.matcher(sb).find()) break;
        }
        end[0] = j;
        return splitVerseRef(sb.toString());
    }

    void fillContent(LinearLayout container, String html, String dayKey) {
        mFillKey = dayKey;
        mBlockIdx = 0;
        List<Blk> bl = parseBlocks(html);
        int qn = 0;
        for (int i = 0; i < bl.size(); i++) {
            Blk b = bl.get(i);
            String low = b.pl.toLowerCase(Locale.ROOT);
            // Tsianjery : en couleur (Lesona Lehibe, Tanora Zokiny ...)
            if (low.startsWith("tsianjery")) {
                int[] end = new int[1];
                String[] tj = scanTsianjery(bl, i, end);
                if (tj != null && !tj[0].isEmpty()) {
                    addTsianjeryCard(container, tj[0], tj[1]);
                    i = end[0] - 1;
                    continue;
                }
            }
            // Référence seule -> asehoy ny texte avy amin'ny Baiboly
            if (!b.tag.startsWith("h") && isBareRef(b.pl)) {
                String next = i + 1 < bl.size() ? bl.get(i + 1).pl : "";
                if (addRefCard(container, b.pl, next)) continue;
            }
            boolean isQ = !b.tag.startsWith("h") && (b.inner.toLowerCase(Locale.ROOT).contains("<code")
                    || b.attrs.contains("question")
                    || (b.tag.equals("p") && low.startsWith("vakio ny ") && b.pl.contains("?")));
            if (isQ) addQuestion(container, b.inner, dayKey + "#" + (qn++));
            else addBlock(container, b.tag, b.inner);
        }
        if (qn == 0) {
            TextView lbl = tv("NY NOTIKO", 12, mThMuted, true);
            lbl.setLetterSpacing(0.1f);
            container.addView(lbl, lpm(MATCH, WRAP, 0, dp(18), 0, dp(8)));
            container.addView(noteBox(dayKey + "#end"), new LinearLayout.LayoutParams(MATCH, WRAP));
        }
        View divider = new View(this);
        divider.setBackgroundColor(mYouth ? 0x22000000 : C_LINE);
        container.addView(divider, lpm(MATCH, dp(1), 0, dp(26), 0, dp(12)));
        TextView footer = tv("Novolavolaina ho anao\nSitraka Nambinintsoa", 12, mThMuted, false);
        footer.setTypeface(null, Typeface.ITALIC);
        footer.setGravity(Gravity.CENTER);
        container.addView(footer, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SURLIGNEUR (Lokoina) — dia voatahiry toy ny note
    // ══════════════════════════════════════════════════════════════════════
    static class HlBg extends android.text.style.BackgroundColorSpan { HlBg() { super(0xFFFFE04D); } }
    static class HlFg extends android.text.style.ForegroundColorSpan { HlFg() { super(0xFF1A1A1A); } }
    static final int ID_HL = 7001, ID_UNHL = 7002;
    int mBlockIdx = 0;
    String mFillKey = "";

    List<int[]> loadHl(String key) {
        List<int[]> out = new ArrayList<>();
        String s = mPrefs.getString("hl:" + key, "");
        if (s.isEmpty()) return out;
        for (String part : s.split(",")) {
            String[] ab = part.split("-");
            if (ab.length != 2) continue;
            try { out.add(new int[]{Integer.parseInt(ab[0]), Integer.parseInt(ab[1])}); }
            catch (Exception ignored) { }
        }
        return out;
    }

    void saveHl(String key, List<int[]> r) {
        SharedPreferences.Editor ed = mPrefs.edit();
        if (r.isEmpty()) {
            ed.remove("hl:" + key);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int[] x : r) {
                if (sb.length() > 0) sb.append(',');
                sb.append(x[0]).append('-').append(x[1]);
            }
            ed.putString("hl:" + key, sb.toString());
        }
        ed.apply();
    }

    List<int[]> hlAdd(List<int[]> r, int a, int b) {
        List<int[]> out = new ArrayList<>();
        int na = a, nb = b;
        for (int[] x : r) {
            if (x[1] < na || x[0] > nb) out.add(x);
            else { na = Math.min(na, x[0]); nb = Math.max(nb, x[1]); }
        }
        out.add(new int[]{na, nb});
        Collections.sort(out, (p, q) -> p[0] - q[0]);
        return out;
    }

    List<int[]> hlRemove(List<int[]> r, int a, int b) {
        List<int[]> out = new ArrayList<>();
        for (int[] x : r) {
            if (x[1] <= a || x[0] >= b) { out.add(x); continue; }
            if (x[0] < a) out.add(new int[]{x[0], a});
            if (x[1] > b) out.add(new int[]{b, x[1]});
        }
        return out;
    }

    void applyHl(TextView tv, List<int[]> r) {
        CharSequence cs = tv.getText();
        if (!(cs instanceof Spannable)) return;
        Spannable sp = (Spannable) cs;
        for (HlBg s : sp.getSpans(0, sp.length(), HlBg.class)) sp.removeSpan(s);
        for (HlFg s : sp.getSpans(0, sp.length(), HlFg.class)) sp.removeSpan(s);
        for (int[] x : r) {
            int a = Math.max(0, x[0]), b = Math.min(sp.length(), x[1]);
            if (b > a) {
                sp.setSpan(new HlBg(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sp.setSpan(new HlFg(), a, b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    TextView bodyView(SpannableStringBuilder sb, int color, int sp, final String hlKey) {
        final TextView t = new TextView(this);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setLineSpacing(dp(4), 1f);
        if (hlKey != null) t.setTextIsSelectable(true);   // copier / sélection système
        t.setText(sb, TextView.BufferType.SPANNABLE);
        t.setMovementMethod(LinkMovementMethod.getInstance());  // versets cliquables
        t.setHighlightColor(0x665B7CFF);
        if (hlKey != null) {
            applyHl(t, loadHl(hlKey));
            t.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override public boolean onCreateActionMode(ActionMode m, Menu menu) {
                    menu.add(0, ID_HL, 0, "Lokoina");
                    menu.add(0, ID_UNHL, 1, "Esory ny loko");
                    return true;
                }
                @Override public boolean onPrepareActionMode(ActionMode m, Menu menu) { return false; }
                @Override public boolean onActionItemClicked(ActionMode m, MenuItem item) {
                    int id = item.getItemId();
                    if (id != ID_HL && id != ID_UNHL) return false;
                    int a = Math.min(t.getSelectionStart(), t.getSelectionEnd());
                    int b = Math.max(t.getSelectionStart(), t.getSelectionEnd());
                    if (a >= 0 && b > a) {
                        List<int[]> r = loadHl(hlKey);
                        r = id == ID_HL ? hlAdd(r, a, b) : hlRemove(r, a, b);
                        saveHl(hlKey, r);
                        applyHl(t, r);
                    }
                    m.finish();
                    return true;
                }
                @Override public void onDestroyActionMode(ActionMode m) { }
            });
        }
        return t;
    }

    String nextHlKey() { return mFillKey + "#b" + (mBlockIdx++); }

    TextView pill(String text) {
        TextView t = tv(text, Math.max(15, mTextSp), 0xFFFFFFFF, true);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{mThAccent, 0xFFE8452C});
        g.setCornerRadius(dp(28));
        t.setBackgroundDrawable(g);
        t.setPadding(dp(22), dp(13), dp(24), dp(13));
        return t;
    }

    void addBlock(LinearLayout c, String tag, String inner) {
        if (tag.startsWith("h")) {
            String txt = plain(inner);
            if (mYouth) {
                c.addView(pill(txt), lpm(WRAP, WRAP, 0, dp(14), 0, dp(14)));
            } else {
                c.addView(tv(txt, mTextSp + 3, mThText, true), lpm(MATCH, WRAP, 0, dp(14), 0, dp(8)));
            }
            return;
        }
        SpannableStringBuilder sb = rich(inner, mThLink);
        if (tag.equals("li")) sb.insert(0, "•  ");
        autoLink(sb, mThLink);
        velominFallback(sb);
        if (tag.equals("blockquote")) {
            if (mYouth) { addYouthBox(c, sb); return; }
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.HORIZONTAL);
            View bar = new View(this);
            bar.setBackgroundColor(C_GOLD);
            box.addView(bar, new LinearLayout.LayoutParams(dp(3), MATCH));
            TextView t = bodyView(sb, mDark ? 0xFFC9CAD6 : 0xFF4A4E63, mTextSp - 1, nextHlKey());
            t.setTypeface(null, Typeface.ITALIC);
            t.setPadding(dp(12), dp(2), 0, dp(2));
            box.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
            c.addView(box, lpm(MATCH, WRAP, 0, dp(4), 0, dp(12)));
        } else {
            c.addView(bodyView(sb, mThText, mTextSp, nextHlKey()), lpm(MATCH, WRAP, 0, 0, 0, dp(12)));
        }
    }

    /** Boîte "Tantara fototra : ..." tahaka ny app ofisialy (Zatovo). */
    void addYouthBox(LinearLayout c, SpannableStringBuilder sb) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundDrawable(rrs(0xFFF5F5F5, dp(20), dp(3), mThAccent));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setBackgroundDrawable(topRounded(mThAccent, dp(17)));
        head.setPadding(dp(14), dp(4), dp(14), dp(4));
        head.addView(new Ico(this, Ico.BOOK), new LinearLayout.LayoutParams(dp(28), dp(28)));
        box.addView(head, new LinearLayout.LayoutParams(MATCH, dp(38)));
        TextView t = bodyView(sb, 0xFF2A2230, mTextSp, nextHlKey());
        t.setPadding(dp(16), dp(12), dp(16), dp(14));
        box.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        c.addView(box, lpm(MATCH, WRAP, 0, dp(6), 0, dp(16)));
    }

    /** Carte "Tsianjery" en couleur. */
    void addTsianjeryCard(LinearLayout c, String verse, String ref) {
        int accent = mYouth ? mThAccent : C_GOLD;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundDrawable(rrs(mYouth ? 0xFFFFFFFF : (mDark ? 0xFF262012 : 0xFFFFF6DD), dp(20), dp(2), mYouth ? accent : 0x99D4A017));
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        TextView lbl = tv("✦  TSIANJERY", 13, accent, true);
        lbl.setLetterSpacing(0.12f);
        card.addView(lbl, new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView v = bodyView(new SpannableStringBuilder("“" + verse + "”"), mThText, mTextSp, nextHlKey());
        v.setTypeface(null, Typeface.ITALIC);
        card.addView(v, lpm(MATCH, WRAP, 0, dp(10), 0, 0));
        if (ref != null && !ref.isEmpty()) {
            SpannableStringBuilder rf = new SpannableStringBuilder("— " + ref);
            rf.setSpan(new VerseSpan(ref, accent), 2, rf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView r = bodyView(rf, accent, Math.max(14, mTextSp - 1), null);
            r.setTypeface(null, Typeface.BOLD);
            card.addView(r, lpm(MATCH, WRAP, 0, dp(8), 0, 0));
        }
        c.addView(card, lpm(MATCH, WRAP, 0, dp(8), 0, dp(16)));
    }

    /** Isan'andininy : ny laharana (1, 2, 3 …) dia loko hafa, mba tsy hifangaro amin'ny isa ao amin'ny texte. */
    SpannableStringBuilder numbered(String text, int numColor) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        Matcher m = Pattern.compile("(?m)^(\\d+)(?=  )").matcher(text);
        while (m.find()) {
            sb.setSpan(new android.text.style.ForegroundColorSpan(numColor), m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.RelativeSizeSpan(0.82f), m.start(1), m.end(1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }

    /** Référence seule -> texte avy amin'ny Baiboly ao anatiny, ary ny référence AMBANINY. */
    boolean addRefCard(LinearLayout c, String ref, String nextPl) {
        List<String[]> res = BibleDb.lookupAll(this, ref);
        if (res.isEmpty()) return false;
        boolean any = false;
        for (String[] r : res) if (r[1] != null && !r[1].isEmpty()) any = true;
        if (!any) return false;
        String key = letters(res.get(0)[1]);
        if (key.length() > 18 && letters(nextPl).contains(key.substring(0, 18))) return false; // efa misy ny texte
        int accent = mYouth ? mThAccent : C_ACCENT;
        int numColor = mYouth ? 0xFFE8452C : C_GOLD;
        for (String[] r : res) {
            if (r[1] == null || r[1].isEmpty()) continue;
            TextView t = bodyView(numbered(r[1], numColor), mThText, mTextSp, nextHlKey());
            t.setTypeface(null, Typeface.ITALIC);
            c.addView(t, lpm(MATCH, WRAP, 0, dp(6), 0, dp(6)));
            SpannableStringBuilder rf = new SpannableStringBuilder("— " + r[0]);
            rf.setSpan(new VerseSpan(r[0], accent), 2, rf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView refTv = bodyView(rf, accent, Math.max(14, mTextSp - 1), null);
            refTv.setTypeface(null, Typeface.BOLD);
            c.addView(refTv, lpm(MATCH, WRAP, 0, 0, 0, dp(16)));
        }
        return true;
    }

    void addQuestion(LinearLayout c, String inner, String noteKey) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundDrawable(mYouth ? rrs(0xCCFFFFFF, dp(20), dp(2), mThAccent) : rr(C_QBOX, dp(20)));
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        SpannableStringBuilder sb = rich(inner, mYouth ? mThAccent : C_QLINK);
        autoLink(sb, mYouth ? mThAccent : C_QLINK);
        TextView t = bodyView(sb, mYouth ? 0xFF2A2230 : (mDark ? 0xFFFFFFFF : 0xFF0F2A4A), mTextSp, nextHlKey());
        t.setTypeface(null, Typeface.BOLD);
        box.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));
        box.addView(noteBox(noteKey), lpm(MATCH, WRAP, 0, dp(14), 0, 0));
        c.addView(box, lpm(MATCH, WRAP, 0, dp(8), 0, dp(16)));
    }

    EditText noteBox(final String key) {
        EditText et = new EditText(this);
        et.setHint("Soraty eto ny hevitrao…");
        et.setHintTextColor(mYouth ? 0x88000000 : 0xFF6E7488);
        et.setTextColor(mYouth ? 0xFF2A2230 : C_TEXT);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(14, mTextSp - 2));
        et.setMinLines(3);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setBackgroundDrawable(rr(mYouth ? 0xAAFFFFFF : C_NOTE, dp(14)));
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setText(mPrefs.getString("note:" + key, ""));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                SharedPreferences.Editor ed = mPrefs.edit();
                if (s.length() == 0) ed.remove("note:" + key);
                else ed.putString("note:" + key, s.toString());
                ed.apply();
            }
        });
        return et;
    }

    // ── Popup andininy Baiboly ─────────────────────────────────────────────
    void showBiblePopup(final String ref) {
        final Dialog dlg = newSheet(false);
        final SwipeSheet root = new SwipeSheet(this);
        root.setBackgroundDrawable(topRounded(C_SHEET, dp(26)));
        root.setPadding(dp(22), dp(4), dp(22), dp(22));
        root.addView(handleBox(), new LinearLayout.LayoutParams(MATCH, dp(36)));
        root.onDismiss = dlg::dismiss;
        final LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(body, new FrameLayout.LayoutParams(MATCH, WRAP));
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, WRAP));
        root.scroller = sv;
        body.addView(new ProgressBar(this));
        showSheet(dlg, root, WRAP);
        new Thread(() -> {
            final List<String[]> res = BibleDb.lookupAll(this, ref);
            mHandler.post(() -> {
                body.removeAllViews();
                if (res.isEmpty()) {
                    body.addView(tv("Tsy hita ny andininy: " + ref, 16, C_MUTED, false));
                    return;
                }
                for (String[] r : res) {
                    body.addView(tv(r[0], mTextSp, C_ACCENT, true), lpm(MATCH, WRAP, 0, dp(6), 0, dp(6)));
                    TextView t = new TextView(this);
                    t.setText(r[1].isEmpty() ? "(tsy hita ny andininy)" : numbered(r[1], C_GOLD));
                    t.setTextColor(C_TEXT);
                    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSp);
                    t.setLineSpacing(dp(4), 1f);
                    t.setTextIsSelectable(true);
                    body.addView(t, lpm(MATCH, WRAP, 0, 0, 0, dp(14)));
                }
            });
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // TSIANJERY 13 (Lesona Lehibe + Zatovo)
    // ══════════════════════════════════════════════════════════════════════
    void buildTsianjery(final Quarter q, final List<Lesson> lessons) {
        final int tok = ++mPageToken;
        mPage = 5;
        mReaderContent = null;
        mShareText = "";
        mRoot.removeAllViews();
        mDrawer = null;
        final FrameLayout page = new FrameLayout(this);
        page.setBackgroundColor(C_BG);
        mRoot.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));
        final ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        page.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col, new FrameLayout.LayoutParams(MATCH, WRAP));

        FrameLayout hero = new FrameLayout(this);
        col.addView(hero, new LinearLayout.LayoutParams(MATCH, dp(320)));
        ImageView heroImg = addHeroImage(hero);
        if (matchesSokajy(q.id, 0)) loadHeroCrop(heroImg, q.cover);
        else loadInto(heroImg, q.cover, 1080);
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setGravity(Gravity.CENTER_HORIZONTAL);
        ht.setPadding(dp(24), 0, dp(24), dp(30));
        hero.addView(ht, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));
        TextView t1 = tv("TSIANJERY " + lessons.size(), 26, C_GOLD, true);
        t1.setGravity(Gravity.CENTER);
        t1.setLetterSpacing(0.05f);
        ht.addView(t1, new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView t2 = tv(q.title, 17, 0xFFFFFFFF, true);
        t2.setGravity(Gravity.CENTER);
        ht.addView(t2, lpm(MATCH, WRAP, 0, dp(8), 0, 0));
        TextView t3 = tv(q.human, 15, 0xB3FFFFFF, false);
        t3.setGravity(Gravity.CENTER);
        ht.addView(t3, lpm(MATCH, WRAP, 0, dp(4), 0, 0));

        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(18), dp(12), dp(18), dp(60));
        col.addView(list, new LinearLayout.LayoutParams(MATCH, WRAP));
        ProgressBar pb = new ProgressBar(this);
        list.addView(pb, lpm(WRAP, WRAP, 0, dp(24), 0, 0));
        ((LinearLayout.LayoutParams) pb.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;

        LinearLayout row = makeBar(page, scroll, 260);
        row.addView(iconBtn(Ico.BACK, v -> buildHome(q.id)), new LinearLayout.LayoutParams(dp(44), dp(44)));
        row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        row.addView(iconBtn(Ico.SHARE, v -> shareTsianjery()), new LinearLayout.LayoutParams(dp(44), dp(44)));

        new Thread(() -> {
            StringBuilder share = new StringBuilder(q.title + "\nTsianjery " + lessons.size() + "\n");
            for (int i = 0; i < lessons.size(); i++) {
                if (tok != mPageToken) return;
                final Lesson ls = lessons.get(i);
                String lj = fetchApi(q.id + "/lessons/" + ls.id + "/index.json", FOREVER);
                String dayId = "01";
                if (lj != null) {
                    try {
                        JSONArray da = new JSONObject(lj).optJSONArray("days");
                        if (da != null && da.length() > 0) dayId = da.getJSONObject(0).optString("id", "01");
                    } catch (Exception ignored) { }
                }
                String cj = fetchApi(q.id + "/lessons/" + ls.id + "/days/" + dayId + "/read/index.json", FOREVER);
                String[] got = null;
                if (cj != null) {
                    try { got = extractTsianjery(new JSONObject(cj).optString("content", "")); }
                    catch (Exception ignored) { }
                }
                final String[] res = got;
                final int idx = i;
                share.append("\nLesona ").append(i + 1).append(": ").append(ls.title).append('\n');
                if (res != null) {
                    share.append(res[0]);
                    if (!res[1].isEmpty()) share.append("\n— ").append(res[1]);
                    share.append('\n');
                }
                mShareText = share.toString();
                mHandler.post(() -> {
                    if (tok != mPageToken) return;
                    if (idx == 0) list.removeAllViews();
                    list.addView(tsianjeryItem(idx + 1, ls.title, res));
                });
            }
        }).start();
    }

    View tsianjeryItem(int n, String title, String[] res) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setLayoutParams(lpm(MATCH, WRAP, 0, 0, 0, dp(18)));
        SpannableStringBuilder h = new SpannableStringBuilder("Lesona " + n + ": ");
        h.setSpan(new android.text.style.ForegroundColorSpan(C_ACCENT), 0, h.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int s = h.length();
        h.append(title);
        h.setSpan(new android.text.style.ForegroundColorSpan(C_TEXT), s, h.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView head = new TextView(this);
        head.setText(h);
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        head.setTypeface(null, Typeface.BOLD);
        item.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundDrawable(rrs(C_CARD, dp(16), dp(1), C_LINE));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (res == null || res[0].isEmpty()) {
            card.addView(tv("(tsy hita ny tsianjery)", 14, C_MUTED, false));
        } else {
            TextView v = tv("“" + res[0] + "”", Math.max(14, mTextSp - 1), C_TEXT, false);
            v.setTypeface(null, Typeface.ITALIC);
            v.setLineSpacing(dp(3), 1f);
            card.addView(v, new LinearLayout.LayoutParams(MATCH, WRAP));
            if (!res[1].isEmpty()) {
                SpannableStringBuilder rf = new SpannableStringBuilder("— " + res[1]);
                rf.setSpan(new VerseSpan(res[1], C_GOLD), 2, rf.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                TextView r = bodyView(rf, C_GOLD, Math.max(14, mTextSp - 2), null);
                r.setTypeface(null, Typeface.BOLD);
                card.addView(r, lpm(MATCH, WRAP, 0, dp(8), 0, 0));
            }
        }
        item.addView(card, lpm(MATCH, WRAP, 0, dp(8), 0, 0));
        return item;
    }

    void shareTsianjery() {
        if (mShareText.isEmpty()) { toast("Miandrasa kely…"); return; }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, mShareText);
        startActivity(Intent.createChooser(i, "Zarao ny Tsianjery"));
    }

    /** Hakana ny tsianjery (andininy + référence) ao amin'ny fizarana Sabata.
     *  Lesona Lehibe/Tanora : "Tsianjery". Zatovo : "Andininy fototra". */
    String[] extractTsianjery(String html) {
        List<Blk> bl = parseBlocks(html);
        for (int i = 0; i < bl.size(); i++) {
            if (bl.get(i).pl.toLowerCase(Locale.ROOT).startsWith("tsianjery")) {
                String[] r = scanTsianjery(bl, i, new int[1]);
                if (r != null && !r[0].isEmpty()) return r;
            }
        }
        for (int i = 0; i < bl.size(); i++) {
            String t = bl.get(i).pl, lt = t.toLowerCase(Locale.ROOT);
            if (!lt.startsWith("andininy fototra")) continue;
            String first = t.substring(Math.min(16, t.length())).replaceFirst("^[\\s:\\-–—]+", "");
            if (!first.isEmpty()) {
                String[] r = splitVerseRef(first);
                if (r != null && !r[1].isEmpty()) return r;
            }
            for (int j = i + 1; j < bl.size() && j <= i + 8; j++) {
                Blk b = bl.get(j);
                String l2 = b.pl.toLowerCase(Locale.ROOT);
                if (l2.startsWith("tantara fototra") || l2.startsWith("fanampim")) continue;
                if (b.tag.startsWith("h")) break;
                if (isBareRef(b.pl)) {
                    String txt = dbText(b.pl);
                    return new String[]{txt, b.pl.trim()};
                }
                if (REF_FULL.matcher(b.pl).find()) {
                    String[] r = splitVerseRef(b.pl);
                    if (r != null) return r;
                }
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FANAVAOZANA AUTOMATIKA (offline) — popup misy %
    // ══════════════════════════════════════════════════════════════════════
    volatile boolean mSyncManual = false;

    void startAutoSync(final boolean manual) {
        if (!isOnline()) {
            if (manual) toast("Tsy misy Internet");
            return;
        }
        if (!mSyncing.compareAndSet(false, true)) {
            if (manual) toast("Efa mandeha ny fanavaozana");
            return;
        }
        mSyncHidden = false;
        mSyncDidWork = false;
        mSyncManual = manual;
        new Thread(() -> {
            try {
                runSync(manual);
            } catch (Throwable t) {
                mLastErr = String.valueOf(t);
            } finally {
                mSyncing.set(false);
                mHandler.post(() -> finishSync());
            }
        }).start();
    }

    void runSync(final boolean manual) throws Exception {
        String idx = fetchApi("index.json", 0);
        if (idx == null) return;
        final List<Quarter> cands = new ArrayList<>();
        final Set<String> newest = new HashSet<>();
        boolean needDlg = manual;
        for (int s = 0; s < 3; s++) {
            List<Quarter> qs = parseQuarters(idx, s);
            for (int k = 0; k < qs.size(); k++) {
                Quarter q = qs.get(k);
                boolean isNew = k < 2;
                if (isNew) newest.add(q.id);
                boolean done = mPrefs.getBoolean("done:" + q.id, false);
                if (!done) needDlg = true;
                if (!done || isNew) cands.add(q);
            }
        }
        Collections.sort(cands, (a, b) -> b.id.compareTo(a.id));   // ny vaovao aloha
        if (needDlg) mHandler.post(() -> showSyncDlg());

        final ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            final Set<String> failedQ = Collections.synchronizedSet(new HashSet<String>());

            // vague A : index ny telovolana rehetra miaraka
            List<Callable<String>> ja = new ArrayList<>();
            for (final Quarter q : cands) {
                ja.add(() -> fetchApi(q.id + "/index.json",
                        newest.contains(q.id) ? (manual ? 0 : STRUCT_AGE) : FOREVER));
            }
            List<Future<String>> fa = pool.invokeAll(ja);

            // vague B : index ny lesona rehetra miaraka
            final Set<String> imgs = new HashSet<>();
            List<Callable<List<String>>> jb = new ArrayList<>();
            for (int i = 0; i < cands.size(); i++) {
                final Quarter q = cands.get(i);
                String qj = fa.get(i).get();
                if (qj == null) { failedQ.add(q.id); continue; }
                final boolean isNew = newest.contains(q.id);
                final long age = isNew ? (manual ? 0 : STRUCT_AGE) : FOREVER;
                if (!imgCached(q.cover)) imgs.add(q.cover);
                for (final Lesson l : parseLessons(qj)) {
                    if (!imgCached(l.cover)) imgs.add(l.cover);
                    jb.add(() -> {
                        List<String> out = new ArrayList<>();
                        String lj = fetchApi(q.id + "/lessons/" + l.id + "/index.json", age);
                        if (lj == null) { out.add("!" + q.id); return out; }
                        JSONArray days = new JSONObject(lj).optJSONArray("days");
                        if (days != null) {
                            for (int d = 0; d < days.length(); d++) {
                                String did = days.getJSONObject(d).optString("id", String.format(Locale.US, "%02d", d + 1));
                                String rel = q.id + "/lessons/" + l.id + "/days/" + did + "/read/index.json";
                                if (!cacheExists(rel) || (isNew && cacheAge(rel) > 3L * 86400000L)) out.add(rel);
                            }
                        }
                        return out;
                    });
                }
            }
            final List<String> rels = new ArrayList<>();
            for (Future<List<String>> f : pool.invokeAll(jb)) {
                for (String x : f.get()) {
                    if (x.startsWith("!")) failedQ.add(x.substring(1));
                    else rels.add(x);
                }
            }

            final int total = rels.size() + imgs.size();
            if (total == 0) {
                markDone(cands, failedQ);
                return;
            }
            mSyncDidWork = true;
            mHandler.post(() -> showSyncDlg());

            // vague C : fampidinana miaraka
            final AtomicInteger doneCnt = new AtomicInteger(0), fails = new AtomicInteger(0);
            final CountDownLatch latch = new CountDownLatch(total);
            for (final String rel : rels) {
                pool.execute(() -> {
                    try {
                        String r = httpGet(API + rel);
                        if (r != null) writeCache(rel, r);
                        else fails.incrementAndGet();
                    } finally {
                        tick(doneCnt, total);
                        latch.countDown();
                    }
                });
            }
            for (final String u : imgs) {
                pool.execute(() -> {
                    try { downloadImage(u); }
                    finally {
                        tick(doneCnt, total);
                        latch.countDown();
                    }
                });
            }
            latch.await();
            if (fails.get() == 0) markDone(cands, failedQ);
        } finally {
            pool.shutdown();
        }
    }

    void markDone(List<Quarter> cands, Set<String> failedQ) {
        SharedPreferences.Editor ed = mPrefs.edit();
        for (Quarter q : cands) if (!failedQ.contains(q.id)) ed.putBoolean("done:" + q.id, true);
        ed.putLong("last_sync", System.currentTimeMillis());
        ed.apply();
    }

    void tick(AtomicInteger c, final int total) {
        final int d = c.incrementAndGet();
        if (d % 4 == 0 || d == total) mHandler.post(() -> updateSync(d, total));
    }

    void showSyncDlg() {
        if (mSyncDlg != null || mSyncHidden || isFinishing()) return;
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackgroundDrawable(rr(C_SHEET, dp(28)));
        box.setPadding(dp(24), dp(26), dp(24), dp(14));
        TextView ttl = tv("Fanavaozana ny lesona", 18, C_TEXT, true);
        ttl.setGravity(Gravity.CENTER);
        box.addView(ttl, new LinearLayout.LayoutParams(MATCH, WRAP));
        mSyncRing = new RingView(this, C_ACCENT, mDark ? 0xFF2B2C3A : 0xFFE0E3EC, C_TEXT);
        box.addView(mSyncRing, lpm(dp(136), dp(136), 0, dp(20), 0, dp(16)));
        mSyncTxt = tv("Mikaroka lesona vaovao…", 13, C_MUTED, false);
        mSyncTxt.setGravity(Gravity.CENTER);
        box.addView(mSyncTxt, new LinearLayout.LayoutParams(MATCH, WRAP));
        TextView hide = tv("Afeno any aoriana", 14, C_ACCENT, true);
        hide.setGravity(Gravity.CENTER);
        hide.setPadding(dp(16), dp(16), dp(16), dp(8));
        hide.setOnClickListener(v -> {
            mSyncHidden = true;
            try { d.dismiss(); } catch (Exception ignored) { }
            mSyncDlg = null;
        });
        box.addView(hide, lpm(WRAP, WRAP, 0, dp(6), 0, 0));
        d.setContentView(box);
        d.setCancelable(false);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout(dp(300), WRAP);
        }
        d.show();
        mSyncDlg = d;
    }

    void updateSync(int done, int total) {
        if (mSyncDlg == null || mSyncRing == null) return;
        mSyncRing.setProgress(total <= 0 ? 0f : done / (float) total);
        mSyncTxt.setText("Mampidina lesona hampiasaina offline · " + done + "/" + total);
    }

    void finishSync() {
        if (mSyncDlg != null) {
            try { mSyncDlg.dismiss(); } catch (Exception ignored) { }
            mSyncDlg = null;
        }
        if (mSyncDidWork) toast("Vita ny fanavaozana ✓ Azo vakiana offline ny lesona");
        else if (mSyncManual) toast("Efa vaovao ny lesona rehetra ✓");
    }
}
