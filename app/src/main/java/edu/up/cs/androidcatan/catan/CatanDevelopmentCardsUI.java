package edu.up.cs.androidcatan.catan;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import edu.up.cs.androidcatan.R;
import edu.up.cs.androidcatan.catan.gamestate.DevelopmentCard;

/**
 * Private illustrated development-card hand. Uses only the existing rules
 * engine and Player state; no change to saved CatanGameState serialization.
 */
public final class CatanDevelopmentCardsUI {
    private CatanDevelopmentCardsUI() { }

    public interface Listener {
        void buyCard();
        void playCard(int cardId);
    }

    private static final String[] TITLES = {
            "Knight", "Victory Point", "Year of Plenty", "Monopoly", "Road Building"
    };
    private static final String[] DESCRIPTIONS = {
            "Move the robber, then steal a random resource from a neighbouring opponent. Knights count towards Largest Army.",
            "A secret victory point, counted automatically. You never need to play this card.",
            "Take two resource cards of your choice from the bank, if they are available.",
            "Choose one resource and collect all cards of that type from the other players.",
            "Build two roads for free, following the normal road placement rules."
    };
    private static final int[] COLORS = {
            0xFF527B91, 0xFF9E744A, 0xFFC29C4F, 0xFF785D92, 0xFF5B855F
    };

    private static int px(Activity a, float dp) {
        return (int) (a.getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    private static android.graphics.drawable.GradientDrawable panel(int color, int radiusPx) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radiusPx);
        return shape;
    }

    private static TextView label(Activity a, String value, int sp, int color, boolean bold) {
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private static LinearLayout vertical(Activity a) {
        LinearLayout layout = new LinearLayout(a);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static void showCollection(final Activity activity, CatanGameState game,
                                      int playerId, boolean purchasePending,
                                      final Listener listener) {
        if (activity == null || game == null || playerId < 0
                || playerId >= game.getPlayerList().size()) return;

        final Player hand = game.getPlayerList().get(playerId);
        final boolean myTurn = game.getCurrentPlayerId() == playerId;
        boolean canPurchase = myTurn && !game.isSetupPhase() && game.isActionPhase()
                && !game.isRobberPhase() && !purchasePending
                && game.hasDevelopmentCardsAvailable()
                && hand.hasResourceBundle(DevelopmentCard.resourceCost);
        boolean canPlayAction = myTurn && !game.isSetupPhase() && !game.isRobberPhase()
                && !game.hasPlayedDevelopmentCardThisTurn();

        final int[] owned = new int[5], playable = new int[5];
        for (Integer id : hand.getDevelopmentCards())
            if (id != null && id >= 0 && id < owned.length) owned[id]++;
        for (Integer id : hand.getPlayableDevCards())
            if (id != null && id >= 0 && id < playable.length) playable[id]++;

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout contents = vertical(activity);
        int inset = px(activity, 14);
        contents.setPadding(inset, inset, inset, inset);
        contents.setBackgroundColor(0xFFF3EBD9);
        scroll.addView(contents);

        TextView subtitle = label(activity, "1 wool + 1 grain + 1 ore  •  Random card", 15,
                0xFF473B2C, true);
        contents.addView(subtitle);

        LinearLayout cost = new LinearLayout(activity);
        cost.setGravity(Gravity.CENTER_VERTICAL);
        cost.setPadding(0, px(activity, 7), 0, px(activity, 6));
        addResourceCost(activity, cost, R.drawable.wool_icon_25x25, "Wool");
        addResourceCost(activity, cost, R.drawable.grain_icon_25x25, "Grain");
        addResourceCost(activity, cost, R.drawable.ore_icon_25x25, "Ore");
        contents.addView(cost);

        final Button buy = new Button(activity);
        buy.setText(purchasePending ? "Drawing card…" : "Buy a development card");
        buy.setAllCaps(false);
        buy.setTextColor(Color.WHITE);
        buy.setBackground(panel(canPurchase ? 0xFF286D57 : 0xFF959086, px(activity, 10)));
        buy.setEnabled(canPurchase);
        contents.addView(buy, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, px(activity, 44)));

        String note = purchasePending ? "Waiting for the game to confirm your draw."
                : !myTurn ? "You can inspect your cards now; buy or play on your turn."
                : game.isSetupPhase() ? "Finish both rounds of initial placement before buying."
                : !game.hasDevelopmentCardsAvailable() ? "The 25-card development deck is empty."
                : !game.isActionPhase() ? "Roll the dice before buying. Action cards may be played before rolling."
                : !hand.hasResourceBundle(DevelopmentCard.resourceCost)
                        ? "You need 1 wool, 1 grain and 1 ore to buy a card."
                        : "One new action card cannot be played on the turn you buy it.";
        TextView tip = label(activity, note, 12, 0xFF6B5A44, false);
        tip.setPadding(0, px(activity, 7), 0, px(activity, 10));
        contents.addView(tip);

        TextView inventoryHeading = label(activity, "YOUR DEVELOPMENT CARDS", 16,
                0xFF3E362F, true);
        inventoryHeading.setPadding(0, 0, 0, px(activity, 10));
        contents.addView(inventoryHeading);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Development cards")
                .setView(scroll)
                .setNegativeButton("Back to island", null)
                .create();

        for (int cardId = 0; cardId < TITLES.length; cardId++) {
            final int id = cardId;
            final boolean victory = cardId == 1;
            int quantities = owned[cardId];
            boolean active = canPlayAction && !victory && playable[cardId] > 0;
            LinearLayout row = new LinearLayout(activity);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(px(activity, 8), px(activity, 9),
                    px(activity, 8), px(activity, 9));
            row.setBackground(panel(quantities > 0 ? 0xFFFFFFFF : 0xFFE0D7C5, px(activity, 10)));

            Artwork art = new Artwork(activity, cardId);
            row.addView(art, new LinearLayout.LayoutParams(px(activity, 105), px(activity, 134)));

            LinearLayout detail = vertical(activity);
            detail.setPadding(px(activity, 12), 0, 0, 0);
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));

            detail.addView(label(activity, TITLES[cardId] + "   ×" + quantities, 17,
                    0xFF40352B, true));
            TextView description = label(activity, DESCRIPTIONS[cardId], 12, 0xFF65574B, false);
            description.setPadding(0, px(activity, 3), 0, px(activity, 5));
            detail.addView(description);

            String status = quantities == 0 ? "Not owned"
                    : victory ? "Secret point • already counted"
                    : playable[cardId] == 0 ? "Bought this turn • available next turn"
                    : !canPlayAction ? "Owned • not playable right now"
                    : "Ready to play";
            detail.addView(label(activity, status, 12,
                    active ? 0xFF21654C : 0xFF80654C, true));

            if (quantities > 0 && !victory) {
                Button use = new Button(activity);
                use.setText("Play " + TITLES[cardId]);
                use.setTextSize(12);
                use.setAllCaps(false);
                use.setEnabled(active);
                detail.addView(use, new LinearLayout.LayoutParams(-1, px(activity, 37)));
                use.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                        listener.playCard(id);
                    }
                });
            }

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = px(activity, 9);
            contents.addView(row, cardParams);
        }

        buy.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                listener.buyCard();
            }
        });
        dialog.show();
    }

    public static void showDrawnCard(Activity activity, int cardId) {
        if (activity == null || cardId < 0 || cardId >= TITLES.length) return;
        final LinearLayout body = vertical(activity);
        body.setPadding(px(activity, 20), px(activity, 12),
                px(activity, 20), px(activity, 8));
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        Artwork card = new Artwork(activity, cardId);
        body.addView(card, new LinearLayout.LayoutParams(px(activity, 160), px(activity, 195)));
        TextView description = label(activity, DESCRIPTIONS[cardId], 15, 0xFF544739, false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, px(activity, 10), 0, 0);
        body.addView(description);
        new AlertDialog.Builder(activity)
                .setTitle("You drew: " + TITLES[cardId])
                .setView(body)
                .setPositiveButton("Great!", null)
                .show();
    }

    private static void addResourceCost(Activity activity, LinearLayout row,
                                        int drawable, String title) {
        LinearLayout part = new LinearLayout(activity);
        part.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(activity);
        icon.setImageResource(drawable);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        part.addView(icon, new LinearLayout.LayoutParams(px(activity, 26), px(activity, 26)));
        TextView text = label(activity, " " + title + "  ", 12, 0xFF473B2C, true);
        part.addView(text);
        row.addView(part);
    }

    /** Procedural vector artwork—no borrowed card images or bitmap assets. */
    private static final class Artwork extends View {
        private final int cardId;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Artwork(Activity a, int cardId) {
            super(a);
            this.cardId = cardId;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.save();
            c.scale(getWidth() / 160f, getHeight() / 200f);
            p.setShader(new LinearGradient(0, 0, 140, 200,
                    COLORS[cardId], 0xFF263D42, Shader.TileMode.CLAMP));
            c.drawRoundRect(2, 2, 158, 198, 12, 12, p);
            p.setShader(null);
            p.setColor(0xFFF3E3BB);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            c.drawRoundRect(7, 7, 153, 193, 10, 10, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x33FFFFFF);
            c.drawCircle(80, 87, 56, p);
            p.setColor(0xFFFFF3D8);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("serif", Typeface.BOLD));
            p.setTextSize(15);
            String title = TITLES[cardId].toUpperCase(java.util.Locale.US);
            if (title.length() > 13) p.setTextSize(12);
            c.drawText(title, 80, 26, p);
            p.setStrokeWidth(7);
            p.setStrokeCap(Paint.Cap.ROUND);
            if (cardId == 0) {
                // Shield, helm and raised sword.
                Path shield = new Path();
                shield.moveTo(80, 57); shield.lineTo(119, 74);
                shield.lineTo(110, 125); shield.lineTo(80, 147);
                shield.lineTo(50, 125); shield.lineTo(41, 74); shield.close();
                c.drawPath(shield, p);
                p.setColor(COLORS[cardId]);
                c.drawCircle(80, 98, 16, p);
                p.setColor(0xFFFFF3D8);
                c.drawRect(76, 83, 84, 130, p);
            } else if (cardId == 1) {
                Path star = new Path();
                for (int i = 0; i < 10; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    float radius = i % 2 == 0 ? 56f : 23f;
                    float sx = (float) (80 + radius * Math.cos(a));
                    float sy = (float) (98 + radius * Math.sin(a));
                    if (i == 0) star.moveTo(sx, sy); else star.lineTo(sx, sy);
                }
                star.close(); c.drawPath(star, p);
            } else if (cardId == 2) {
                for (int i = -1; i <= 1; i++) {
                    float sx = 80 + i * 30;
                    c.drawLine(sx, 137, sx, 67, p);
                    c.drawOval(sx - 14, 80, sx + 14, 106, p);
                    c.drawOval(sx - 14, 110, sx + 14, 133, p);
                }
            } else if (cardId == 3) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(9);
                c.drawCircle(80, 97, 43, p);
                c.drawCircle(80, 97, 21, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(80, 97, 8, p);
            } else if (cardId == 4) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(17);
                c.drawLine(44, 141, 70, 108, p);
                c.drawLine(70, 108, 96, 88, p);
                c.drawLine(96, 88, 124, 51, p);
                p.setStyle(Paint.Style.FILL);
                for (float[] dot : new float[][] {{44,141},{70,108},{96,88},{124,51}})
                    c.drawCircle(dot[0], dot[1], 9, p);
            }
            p.setColor(0xFFFFF3D8);
            p.setTextSize(11);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            c.drawText("DEVELOPMENT", 80, 179, p);
            c.restore();
        }
    }
}
