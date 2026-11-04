package com.aeman.haker;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native ordering app for مطعم الرفاعي. Menu, orders and settings are stored locally in SQLite. */
public class MainActivity extends AppCompatActivity {
    private static final String PHONE = "0945254957";
    private static final int ORANGE = Color.rgb(215, 83, 27);
    private static final int INK = Color.rgb(45, 37, 32);
    private Database database;
    private LinearLayout content;
    private final Map<Integer, Integer> basket = new LinkedHashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(ORANGE);
        getWindow().setNavigationBarColor(INK);
        database = new Database(this);
        showMenu();
    }

    private void showMenu() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250, 247, 242));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(22));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        TextView title = text("مطعم الرفاعي", 30, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        TextView welcome = text("أهلاً بكم! نكهة طيبة ووجبة شهية بانتظاركم 🍽️\nاطلبوا وجبتكم المفضلة الآن", 16, Color.WHITE, false);
        welcome.setGravity(Gravity.CENTER);
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(14), dp(18), dp(14), dp(18));
        hero.setBackground(round(ORANGE, 20));
        hero.addView(title);
        hero.addView(welcome);
        content.addView(hero, params(-1, -2, 0, 0, 0, 12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button call = button("اتصل بالمطعم  " + PHONE, ORANGE);
        call.setOnClickListener(v -> dialRestaurant());
        Button checkout = button("السلة (" + basketCount() + ")", INK);
        checkout.setOnClickListener(v -> checkout());
        actions.addView(call, new LinearLayout.LayoutParams(0, dp(48), 1));
        actions.addView(checkout, new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(actions, params(-1, -2, 0, 0, 0, 8));

        TextView section = text("قائمة الوجبات", 22, INK, true);
        section.setGravity(Gravity.RIGHT);
        content.addView(section, params(-1, -2, 0, 0, 0, 5));
        Cursor foods = database.getReadableDatabase().rawQuery("SELECT id,name,price FROM foods WHERE active=1 ORDER BY id", null);
        while (foods.moveToNext()) {
            int id = foods.getInt(0);
            String name = foods.getString(1);
            int price = foods.getInt(2);
            content.addView(foodCard(id, name, price), params(-1, -2, 0, 0, 0, 8));
        }
        foods.close();

        Button admin = button("دخول الإدارة ⚙", INK);
        admin.setOnClickListener(v -> adminLogin());
        content.addView(admin, params(-1, dp(48), 0, 14, 0, 10));
        TextView credits = text("إدارة مطعم الرفاعي: أبو محمود عبد الحي\nتطوير: أيمن العربينية • بلال عبد الحي", 13, Color.GRAY, false);
        credits.setGravity(Gravity.CENTER);
        content.addView(credits, params(-1, -2, 0, 8, 0, 4));
    }

    private View foodCard(int id, String name, int price) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(8), dp(10), dp(8));
        card.setBackground(round(Color.WHITE, 16));
        TextView details = text(name + "\n" + price, 17, INK, true);
        details.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        card.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        Button add = button("أضف للسلة +", ORANGE);
        add.setOnClickListener(v -> {
            basket.put(id, basket.containsKey(id) ? basket.get(id) + 1 : 1);
            Toast.makeText(this, "أضيفت " + name + " إلى السلة", Toast.LENGTH_SHORT).show();
            showMenu();
        });
        card.addView(add, new LinearLayout.LayoutParams(-2, dp(44)));
        return card;
    }

    private void checkout() {
        if (basket.isEmpty()) {
            Toast.makeText(this, "السلة فارغة، أضف وجبة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        int total = 0;
        StringBuilder summary = new StringBuilder();
        SQLiteDatabase db = database.getReadableDatabase();
        for (Map.Entry<Integer, Integer> item : basket.entrySet()) {
            Cursor c = db.rawQuery("SELECT name,price FROM foods WHERE id=? AND active=1", new String[]{String.valueOf(item.getKey())});
            if (c.moveToFirst()) {
                int line = c.getInt(1) * item.getValue();
                total += line;
                summary.append(c.getString(0)).append(" × ").append(item.getValue()).append(" = ").append(line).append("\n");
            }
            c.close();
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(4), dp(8), 0);
        EditText customer = input("اسم الزبون");
        EditText phone = input("رقم الهاتف");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        form.addView(customer);
        form.addView(phone);
        String orderSummary = summary.toString();
        int finalTotal = total;
        new AlertDialog.Builder(this).setTitle("تأكيد الطلب • المجموع " + total)
                .setMessage(orderSummary).setView(form)
                .setNegativeButton("متابعة التسوق", null)
                .setPositiveButton("تأكيد الطلب", (dialog, which) -> {
                    String client = customer.getText().toString().trim();
                    String clientPhone = phone.getText().toString().trim();
                    if (client.isEmpty() || clientPhone.isEmpty()) {
                        Toast.makeText(this, "أدخل الاسم ورقم الهاتف لإتمام الطلب", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ContentValues values = new ContentValues();
                    values.put("customer", client);
                    values.put("phone", clientPhone);
                    values.put("items", orderSummary);
                    values.put("total", finalTotal);
                    values.put("status", "جديد");
                    values.put("created", System.currentTimeMillis());
                    database.getWritableDatabase().insert("orders", null, values);
                    basket.clear();
                    new AlertDialog.Builder(this).setTitle("تم استلام طلبكم 🍽️")
                            .setMessage("شكراً " + client + "! تم حفظ طلبكم. للاستفسار اتصلوا على " + PHONE)
                            .setPositiveButton("حسناً", (d, w) -> showMenu()).show();
                }).show();
    }

    private void dialRestaurant() {
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + PHONE))); }
        catch (Exception e) { Toast.makeText(this, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show(); }
    }

    private void adminLogin() {
        EditText pin = input("رمز الإدارة");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("دخول الإدارة").setView(pin)
                .setNegativeButton("إلغاء", null).setPositiveButton("دخول", (d, w) -> {
                    String expected = getSharedPreferences("restaurant_admin", MODE_PRIVATE).getString("pin", "2468");
                    if (expected.equals(pin.getText().toString())) adminPanel();
                    else Toast.makeText(this, "رمز الإدارة غير صحيح", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void adminPanel() {
        String[] options = {"إدارة الوجبات والأسعار", "إضافة وجبة", "عرض الطلبات", "تغيير رمز الإدارة"};
        new AlertDialog.Builder(this).setTitle("لوحة إدارة مطعم الرفاعي")
                .setItems(options, (d, which) -> {
                    if (which == 0) manageFoods();
                    else if (which == 1) addFood();
                    else if (which == 2) showOrders();
                    else changePin();
                }).setNegativeButton("إغلاق", null).show();
    }

    private void manageFoods() {
        Cursor c = database.getReadableDatabase().rawQuery("SELECT id,name,price,active FROM foods ORDER BY id", null);
        List<Integer> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        while (c.moveToNext()) {
            ids.add(c.getInt(0));
            labels.add((c.getInt(3) == 1 ? "● " : "○ ") + c.getString(1) + " — " + c.getInt(2));
        }
        c.close();
        new AlertDialog.Builder(this).setTitle("اختر وجبة للتعديل")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> foodOptions(ids.get(which)))
                .setPositiveButton("رجوع", (dialog, which) -> adminPanel()).show();
    }

    private void foodOptions(int id) {
        Cursor c = database.getReadableDatabase().rawQuery("SELECT name,price,active FROM foods WHERE id=?", new String[]{String.valueOf(id)});
        if (!c.moveToFirst()) { c.close(); return; }
        String name = c.getString(0);
        int currentPrice = c.getInt(1);
        boolean active = c.getInt(2) == 1;
        c.close();
        String[] options = {"تعديل السعر", active ? "إخفاء الوجبة من القائمة" : "إظهار الوجبة في القائمة", "حذف الوجبة"};
        new AlertDialog.Builder(this).setTitle(name).setItems(options, (d, which) -> {
            if (which == 0) {
                EditText price = input("السعر الجديد");
                price.setInputType(InputType.TYPE_CLASS_NUMBER);
                price.setText(String.valueOf(currentPrice));
                new AlertDialog.Builder(this).setTitle("تعديل السعر").setView(price)
                        .setNegativeButton("إلغاء", null).setPositiveButton("حفظ", (x, y) -> {
                            try {
                                int value = Integer.parseInt(price.getText().toString());
                                ContentValues cv = new ContentValues(); cv.put("price", value);
                                database.getWritableDatabase().update("foods", cv, "id=?", new String[]{String.valueOf(id)});
                                Toast.makeText(this, "تم حفظ السعر", Toast.LENGTH_SHORT).show(); showMenu();
                            } catch (NumberFormatException e) { Toast.makeText(this, "أدخل سعراً صحيحاً", Toast.LENGTH_SHORT).show(); }
                        }).show();
            } else if (which == 1) {
                ContentValues cv = new ContentValues(); cv.put("active", active ? 0 : 1);
                database.getWritableDatabase().update("foods", cv, "id=?", new String[]{String.valueOf(id)});
                showMenu();
            } else new AlertDialog.Builder(this).setMessage("هل تريد حذف " + name + "؟")
                    .setNegativeButton("إلغاء", null).setPositiveButton("حذف", (x, y) -> {
                        database.getWritableDatabase().delete("foods", "id=?", new String[]{String.valueOf(id)}); showMenu();
                    }).show();
        }).show();
    }

    private void addFood() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText name = input("اسم الوجبة");
        EditText price = input("السعر"); price.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(name); form.addView(price);
        new AlertDialog.Builder(this).setTitle("إضافة وجبة جديدة").setView(form)
                .setNegativeButton("إلغاء", null).setPositiveButton("إضافة", (d, w) -> {
                    try {
                        String n = name.getText().toString().trim(); int p = Integer.parseInt(price.getText().toString());
                        if (n.isEmpty() || p < 0) throw new NumberFormatException();
                        ContentValues cv = new ContentValues(); cv.put("name", n); cv.put("price", p); cv.put("active", 1);
                        database.getWritableDatabase().insert("foods", null, cv); showMenu();
                    } catch (NumberFormatException e) { Toast.makeText(this, "تحقق من الاسم والسعر", Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private void showOrders() {
        Cursor c = database.getReadableDatabase().rawQuery("SELECT id,customer,phone,items,total,status FROM orders ORDER BY id DESC", null);
        StringBuilder list = new StringBuilder();
        while (c.moveToNext()) list.append("طلب #").append(c.getInt(0)).append(" • ").append(c.getString(1))
                .append(" ( ").append(c.getString(2)).append(" )\n").append(c.getString(3))
                .append("المجموع: ").append(c.getInt(4)).append(" — ").append(c.getString(5)).append("\n──────────────\n");
        c.close();
        if (list.length() == 0) list.append("لا توجد طلبات محفوظة بعد.");
        new AlertDialog.Builder(this).setTitle("الطلبات المسجلة").setMessage(list.toString())
                .setPositiveButton("رجوع", (d, w) -> adminPanel()).show();
    }

    private void changePin() {
        EditText pin = input("رمز جديد (4 أرقام على الأقل)");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("تغيير رمز الإدارة").setView(pin)
                .setNegativeButton("إلغاء", null).setPositiveButton("حفظ", (d, w) -> {
                    String value = pin.getText().toString();
                    if (value.length() < 4) Toast.makeText(this, "الرمز يجب أن يتكون من 4 أرقام على الأقل", Toast.LENGTH_SHORT).show();
                    else { getSharedPreferences("restaurant_admin", MODE_PRIVATE).edit().putString("pin", value).apply(); Toast.makeText(this, "تم تغيير الرمز", Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private int basketCount() { int count = 0; for (int q : basket.values()) count += q; return count; }
    private EditText input(String hint) { EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setTextSize(16); return e; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(13);
        b.setAllCaps(false); b.setBackground(round(color, 12)); return b;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private LinearLayout.LayoutParams params(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom)); return p;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static class Database extends SQLiteOpenHelper {
        private static final String NAME = "alrifai_restaurant.db";
        private static final int VERSION = 1;
        private final Context context;
        Database(Context context) { super(context, NAME, null, VERSION); this.context = context; }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE foods(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,price INTEGER NOT NULL,active INTEGER NOT NULL DEFAULT 1)");
            db.execSQL("CREATE TABLE orders(id INTEGER PRIMARY KEY AUTOINCREMENT,customer TEXT NOT NULL,phone TEXT NOT NULL,items TEXT NOT NULL,total INTEGER NOT NULL,status TEXT NOT NULL,created INTEGER NOT NULL)");
            String[][] meals = {
                {"كيلو شاورما","1500"},{"نصف كيلو شاورما","750"},{"شاورما خرطوشة","100"},{"شاورما وسط","150"},{"شاورما كبيرة","200"},{"شاورما اكسترا","250"},
                {"وجبة عربي","350"},{"وجبة كريسبي","500"},{"سندويشة عربي","250"},{"صحن بطاطا","250"},{"فروج بروستد","1200"},{"نصف فروج بروستد","600"},
                {"فروج مشوي","1000"},{"وجبة جوانح","300"},{"سندويشة فاهيتا","250"},{"برجر دجاج","150"},{"برجر لحم","350"},{"سندويشة بطاطا","150"},
                {"صحن رز بخاري كبير","500"},{"صحن رز بخاري وسط","350"}
            };
            for (String[] meal : meals) {
                ContentValues values = new ContentValues(); values.put("name", meal[0]); values.put("price", Integer.parseInt(meal[1])); values.put("active", 1);
                db.insert("foods", null, values);
            }
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    }
}
