package com.example;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

final class UserProfileDialog {

    private static final int DIALOG_MIN_WIDTH = 460;
    private static final int AVATAR_SIZE      = 80;

    private UserProfileDialog() {}

    static void show(Window owner, String handle,
                     CodeforcesProfileService service,
                     AppThemePalette palette) {
        AppThemePalette p = palette != null ? palette : AppThemePalette.dark();

        JDialog dialog = new JDialog(owner,
                handle + " — Codeforces Profile",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, 100));

        dialog.setContentPane(buildLoadingPanel(p));
        dialog.setPreferredSize(new Dimension(DIALOG_MIN_WIDTH, 160));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        // Local record to carry both results from the background thread.
        // (Java 16+ feature; this project targets Java 17.)
        record FetchResult(UserProfile profile, BufferedImage avatar) {}

        SwingWorker<FetchResult, Void> worker = new SwingWorker<>() {
            @Override
            protected FetchResult doInBackground() throws Exception {
                UserProfile prof = service.fetchProfile(handle);
                BufferedImage img = service.fetchAvatar(prof.avatarUrl());
                return new FetchResult(prof, img);
            }

            @Override
            protected void done() {
                try {
                    FetchResult r = get();
                    JPanel content = buildProfilePanel(r.profile(), r.avatar(), dialog, p);
                    dialog.setContentPane(content);
                    dialog.setPreferredSize(null);
                    dialog.pack();
                    dialog.setSize(Math.max(dialog.getWidth(), DIALOG_MIN_WIDTH), dialog.getHeight());
                    dialog.setLocationRelativeTo(owner);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    replaceWithError(dialog, owner, "Request was interrupted.", p);
                } catch (ExecutionException e) {
                    String msg = e.getCause() != null ? e.getCause().getMessage() : "Unknown error";
                    replaceWithError(dialog, owner, msg, p);
                }
            }
        };
        worker.execute();

        dialog.setVisible(true); // blocks (modal) — EDT pump keeps done() callable
    }

    // ── Loading panel ────────────────────────────────────────────────────────

    private static JPanel buildLoadingPanel(AppThemePalette p) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(p.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        JLabel label = new JLabel("Loading profile...", SwingConstants.CENTER);
        label.setForeground(p.mutedTextColor());
        label.setFont(label.getFont().deriveFont(13f));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    // ── Error panel ──────────────────────────────────────────────────────────

    private static void replaceWithError(JDialog dialog, Window owner, String msg, AppThemePalette p) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(p.frameBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel label = new JLabel(
                "<html><center>" + htmlEncode(msg) + "</center></html>",
                SwingConstants.CENTER);
        label.setForeground(p.errorColor());
        label.setFont(label.getFont().deriveFont(13f));
        panel.add(label, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(close);
        panel.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setPreferredSize(new Dimension(DIALOG_MIN_WIDTH, 180));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    // ── Profile panel ─────────────────────────────────────────────────────────

    private static JPanel buildProfilePanel(UserProfile profile, BufferedImage avatar,
                                            JDialog dialog, AppThemePalette p) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(p.frameBackground());
        outer.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor  = GridBagConstraints.NORTH;

        gbc.gridy  = 0; gbc.insets = new Insets(0, 0, 8, 0);
        outer.add(buildHeaderCard(profile, avatar, p), gbc);

        gbc.gridy  = 1;
        outer.add(buildAccountCard(profile, p), gbc);

        gbc.gridy  = 2;
        outer.add(buildStatsCard(profile, p), gbc);

        gbc.gridy   = 3;
        gbc.insets  = new Insets(6, 0, 0, 0);
        gbc.weighty = 0;
        outer.add(buildButtonRow(profile.handle(), dialog), gbc);

        return outer;
    }

    // ── Header card: avatar + handle/rank/rating ──────────────────────────────

    private static JPanel buildHeaderCard(UserProfile profile, BufferedImage avatarImg, AppThemePalette p) {
        JPanel card = createCard(p);
        card.setLayout(new BorderLayout(14, 0));

        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setMinimumSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setIcon(avatarImg != null
                ? new ImageIcon(circularCrop(avatarImg, AVATAR_SIZE))
                : placeholderAvatar(AVATAR_SIZE, p));
        card.add(avatarLabel, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new GridBagLayout());

        GridBagConstraints ic = new GridBagConstraints();
        ic.gridx = 0; ic.anchor = GridBagConstraints.WEST; ic.weightx = 1.0;
        ic.fill  = GridBagConstraints.HORIZONTAL;

        ic.gridy = 0; ic.insets = new Insets(2, 0, 2, 0);
        JLabel handleLabel = new JLabel(profile.handle());
        handleLabel.setFont(handleLabel.getFont().deriveFont(Font.BOLD, 17f));
        handleLabel.setForeground(p.textColor());
        info.add(handleLabel, ic);

        if (!profile.rank().isBlank()) {
            ic.gridy = 1; ic.insets = new Insets(1, 0, 1, 0);
            JLabel rankLabel = new JLabel(capitalize(profile.rank()));
            rankLabel.setFont(rankLabel.getFont().deriveFont(Font.BOLD, 13f));
            rankLabel.setForeground(rankColor(profile.rank()));
            info.add(rankLabel, ic);
        }

        if (profile.rating() > 0) {
            ic.gridy = 2; ic.insets = new Insets(3, 0, 2, 0);
            String ratingText = "Rating: " + profile.rating();
            if (profile.maxRating() > profile.rating())
                ratingText += "   (Peak: " + profile.maxRating() + ")";
            JLabel ratingLabel = new JLabel(ratingText);
            ratingLabel.setFont(ratingLabel.getFont().deriveFont(Font.PLAIN, 12f));
            ratingLabel.setForeground(p.mutedTextColor());
            info.add(ratingLabel, ic);
        }

        card.add(info, BorderLayout.CENTER);
        return card;
    }

    // ── Account card ──────────────────────────────────────────────────────────

    private static JPanel buildAccountCard(UserProfile profile, AppThemePalette p) {
        JPanel card = createCard(p);
        card.setLayout(new GridBagLayout());

        GridBagConstraints kc = keyGbc();
        GridBagConstraints vc = valGbc();
        int row = 0;

        addSectionHeader(card, "Account", row++, p);
        addDataRow(card, kc, vc, row++, "Registered", formatDate(profile.registrationTimeSeconds()), p);
        addDataRow(card, kc, vc, row++, "Last online",  timeAgo(profile.lastOnlineTimeSeconds()),     p);
        if (!profile.country().isBlank())
            addDataRow(card, kc, vc, row++, "Country",      profile.country(),                         p);
        if (!profile.organization().isBlank())
            addDataRow(card, kc, vc, row,   "Organization", profile.organization(),                    p);

        return card;
    }

    // ── Statistics card ───────────────────────────────────────────────────────

    private static JPanel buildStatsCard(UserProfile profile, AppThemePalette p) {
        JPanel card = createCard(p);
        card.setLayout(new GridBagLayout());

        GridBagConstraints kc = keyGbc();
        GridBagConstraints vc = valGbc();
        int row = 0;

        addSectionHeader(card, "Statistics", row++, p);
        addDataRow(card, kc, vc, row++, "Problems solved",
                profile.problemsSolved() > 0 ? String.valueOf(profile.problemsSolved()) : "—", p);
        addDataRow(card, kc, vc, row++, "Total submissions",
                profile.totalSubmissions() > 0 ? String.valueOf(profile.totalSubmissions()) : "—", p);
        addDataRow(card, kc, vc, row++, "Current streak",
                profile.currentStreak() > 0 ? pluralize(profile.currentStreak(), "day") : "—", p);
        addDataRow(card, kc, vc, row, "Longest streak",
                profile.longestStreak() > 0 ? pluralize(profile.longestStreak(), "day") : "—", p);

        return card;
    }

    // ── Button row ────────────────────────────────────────────────────────────

    private static JPanel buildButtonRow(String handle, JDialog dialog) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);

        JButton viewBtn = new JButton("View on Codeforces");
        viewBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://codeforces.com/profile/" + handle));
            } catch (Exception ignored) {}
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        row.add(viewBtn);
        row.add(closeBtn);
        return row;
    }

    // ── Layout building blocks ────────────────────────────────────────────────

    private static JPanel createCard(AppThemePalette p) {
        JPanel panel = new JPanel();
        panel.setBackground(p.panelBackground());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(p.borderColor()),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static GridBagConstraints keyGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx  = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 18);
        return c;
    }

    private static GridBagConstraints valGbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx   = 1;
        c.anchor  = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.insets  = new Insets(2, 0, 2, 0);
        return c;
    }

    private static void addSectionHeader(JPanel card, String title, int row, AppThemePalette p) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = row;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 6, 0);
        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setForeground(p.textColor());
        card.add(lbl, c);
    }

    private static void addDataRow(JPanel card, GridBagConstraints kc, GridBagConstraints vc,
                                   int row, String key, String value, AppThemePalette p) {
        kc.gridy = row;
        JLabel k = new JLabel(key);
        k.setFont(k.getFont().deriveFont(Font.PLAIN, 12f));
        k.setForeground(p.mutedTextColor());
        card.add(k, kc);

        vc.gridy = row;
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.PLAIN, 12f));
        v.setForeground(p.textColor());
        card.add(v, vc);
    }

    // ── Avatar helpers ────────────────────────────────────────────────────────

    private static BufferedImage circularCrop(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setClip(new Ellipse2D.Float(0, 0, size, size));
        g2.drawImage(src, 0, 0, size, size, null);
        g2.dispose();
        return out;
    }

    private static ImageIcon placeholderAvatar(int size, AppThemePalette p) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(p.surfaceBackground());
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.setColor(p.mutedTextColor());
        Font f = g2.getFont().deriveFont(Font.BOLD, size * 0.35f);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        String ch = "?";
        g2.drawString(ch, (size - fm.stringWidth(ch)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
        return new ImageIcon(img);
    }

    // ── Rank color ────────────────────────────────────────────────────────────

    private static Color rankColor(String rank) {
        if (rank == null) return Color.GRAY;
        return switch (rank.toLowerCase(Locale.ROOT)) {
            case "newbie"                    -> new Color(0x808080);
            case "pupil"                     -> new Color(0x008000);
            case "specialist"                -> new Color(0x03a89e);
            case "expert"                    -> new Color(0x3333ff);
            case "candidate master"          -> new Color(0xaa00aa);
            case "master"                    -> new Color(0xff8c00);
            case "international master"      -> new Color(0xff8c00);
            case "grandmaster"               -> new Color(0xff3333);
            case "international grandmaster" -> new Color(0xff3333);
            case "legendary grandmaster"     -> new Color(0xff0000);
            default                          -> new Color(0x808080);
        };
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private static String formatDate(long epochSeconds) {
        if (epochSeconds <= 0) return "Unknown";
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH));
    }

    private static String timeAgo(long epochSeconds) {
        if (epochSeconds <= 0) return "Unknown";
        long delta = Instant.now().getEpochSecond() - epochSeconds;
        if (delta < 60)           return "Just now";
        if (delta < 3600)         return pluralize(delta / 60, "minute") + " ago";
        if (delta < 86_400)       return pluralize(delta / 3600, "hour") + " ago";
        if (delta < 86_400L * 30) return pluralize(delta / 86_400, "day") + " ago";
        if (delta < 86_400L * 365) return pluralize(delta / (86_400L * 30), "month") + " ago";
        return pluralize(delta / (86_400L * 365), "year") + " ago";
    }

    private static String pluralize(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String htmlEncode(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
