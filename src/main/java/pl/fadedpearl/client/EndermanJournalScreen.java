package pl.fadedpearl.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import pl.fadedpearl.entity.journal.EndermanJournalSnapshot;
import pl.fadedpearl.entity.journal.EndermanJournalTrustGuide;
import pl.fadedpearl.entity.trust.FadedTrustManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EndermanJournalScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("minecraft", "textures/gui/book.png");
    private static final ResourceLocation BOOK_SPREAD_TEXTURE = new ResourceLocation("faded_pearl", "textures/gui/enderman_journal_spread.png");
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int PAGE_WIDTH = 146;
    private static final int PAGE_HEIGHT = 180;
    private static final int SPREAD_WIDTH = PAGE_WIDTH * 2;
    private static final int TEXT_WIDTH = 116;
    private static final int LINES_PER_PAGE = 13;

    private final EndermanJournalSnapshot snapshot;
    private Chapter chapter = Chapter.DAYS;
    private List<List<FormattedCharSequence>> pages = List.of(List.of());
    private int page;
    private boolean twoPage;
    private int bookLeft;
    private int bookTop;
    private Button previousButton;
    private Button nextButton;

    private enum Chapter {
        DAYS("screen.faded_pearl.journal.tab.days"),
        BEHAVIORS("screen.faded_pearl.journal.tab.behaviors"),
        BASICS("screen.faded_pearl.journal.tab.basics");

        private final String key;
        Chapter(String key) { this.key = key; }
    }

    private EndermanJournalScreen(EndermanJournalSnapshot snapshot) {
        super(Component.translatable("screen.faded_pearl.journal.title"));
        this.snapshot = snapshot;
    }

    public static void open(EndermanJournalSnapshot snapshot) {
        Minecraft.getInstance().setScreen(new EndermanJournalScreen(snapshot));
    }

    @Override
    protected void init() {
        twoPage = width >= SPREAD_WIDTH + 12 && height >= PAGE_HEIGHT + 52;
        bookLeft = (width - (twoPage ? SPREAD_WIDTH : BOOK_WIDTH)) / 2;
        bookTop = twoPage ? Math.max(28, (height - PAGE_HEIGHT - 28) / 2)
                : (height - BOOK_HEIGHT) / 2;
        int available = Math.max(174, Math.min(300, width - 12));
        int tabWidth = available / 3;
        int tabsLeft = (width - tabWidth * 3) / 2;
        Chapter[] values = Chapter.values();
        for (int index = 0; index < values.length; index++) {
            Chapter target = values[index];
            addRenderableWidget(Button.builder(Component.translatable(target.key), button -> select(target))
                    .bounds(tabsLeft + index * tabWidth, Math.max(4, bookTop - 24), tabWidth, 20).build());
        }
        int buttonTop = bookTop + (twoPage ? PAGE_HEIGHT + 4 : 158);
        previousButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(bookLeft + (twoPage ? 4 : 22), buttonTop, 32, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(bookLeft + (twoPage ? SPREAD_WIDTH - 36 : 138), buttonTop, 32, 20).build());
        rebuildPages();
    }

    private void select(Chapter target) {
        chapter = target;
        page = 0;
        rebuildPages();
    }

    private void changePage(int delta) {
        page = JournalPageNavigation.move(page, delta, pages.size(), twoPage);
        updateNavigationButtons();
    }

    private void rebuildPages() {
        List<Component> content = new ArrayList<>();
        content.add(Component.translatable(chapter.key).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
        switch (chapter) {
            case DAYS -> addDays(content);
            case BEHAVIORS -> addBehaviors(content);
            case BASICS -> addBasics(content);
        }
        pages = paginate(content);
        page = JournalPageNavigation.clampStartPage(page, pages.size(), twoPage);
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < JournalPageNavigation.maxStartPage(pages.size(), twoPage);
    }

    private void addDays(List<Component> content) {
        for (EndermanJournalSnapshot.DayEntry entry : EndermanJournalSnapshot.DayEntry.values()) {
            boolean unlocked = snapshot.days().contains(entry);
            addEntry(content, unlocked, "journal.faded_pearl.day." + key(entry));
        }
    }

    private void addBehaviors(List<Component> content) {
        for (EndermanJournalSnapshot.BehaviorEntry entry : EndermanJournalSnapshot.BehaviorEntry.values()) {
            boolean unlocked = snapshot.behaviors().contains(entry);
            addEntry(content, unlocked, "journal.faded_pearl.behavior." + key(entry));
            if (unlocked) EndermanJournalTrustGuide.requiredTrust(entry).ifPresent(required ->
                    content.add(Component.translatable("journal.faded_pearl.behavior.trust_requirement",
                            EndermanJournalTrustGuide.heartNumber(required), required)
                            .withStyle(ChatFormatting.DARK_PURPLE)));
            if (unlocked) {
                content.add(Component.translatable("journal.faded_pearl.behavior.how_to")
                        .withStyle(ChatFormatting.DARK_PURPLE));
                content.add(Component.translatable("journal.faded_pearl.behavior." + key(entry) + ".use"));
                addRecipe(content, entry);
            }
        }
    }

    private void addRecipe(List<Component> content, EndermanJournalSnapshot.BehaviorEntry entry) {
        if (entry != EndermanJournalSnapshot.BehaviorEntry.HOME
                && entry != EndermanJournalSnapshot.BehaviorEntry.ESCAPE_PEARL) return;
        content.add(Component.translatable("journal.faded_pearl.recipe.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE));
        if (entry == EndermanJournalSnapshot.BehaviorEntry.HOME) {
            addRecipeRows(content, "apa", "aca", "aea");
            addRecipeLegend(content, "amethyst", "ender_pearl", "crying_obsidian", "echo_shard");
        }
        else {
            addRecipeRows(content, "mem", "apa", "mcm");
            addRecipeLegend(content, "phantom_membrane", "amethyst", "ender_pearl",
                    "echo_shard", "crying_obsidian");
        }
    }

    private void addRecipeRows(List<Component> content, String... rows) {
        for (String row : rows)
            content.add(Component.translatable("journal.faded_pearl.recipe.row." + row)
                    .withStyle(ChatFormatting.DARK_GRAY));
    }

    private void addRecipeLegend(List<Component> content, String... ingredients) {
        for (String ingredient : ingredients)
            content.add(Component.translatable("journal.faded_pearl.recipe.ingredient." + ingredient)
                    .withStyle(ChatFormatting.GRAY));
    }

    private void addBasics(List<Component> content) {
        content.add(Component.empty());
        content.add(Component.translatable("journal.faded_pearl.basic.trust").withStyle(ChatFormatting.BOLD));
        content.add(Component.translatable("journal.faded_pearl.basic.trust_value",
                snapshot.trust(), FadedTrustManager.MAX_TRUST));
        content.add(Component.translatable("journal.faded_pearl.basic.heart_value",
                EndermanJournalTrustGuide.heartNumber(snapshot.trust()),
                EndermanJournalTrustGuide.heartNumber(FadedTrustManager.MAX_TRUST)));
        content.add(Component.translatable("journal.faded_pearl.basic.loss_hint")
                .withStyle(ChatFormatting.DARK_RED));
        content.add(Component.empty());
        content.add(Component.translatable("journal.faded_pearl.basic.care_title")
                .withStyle(ChatFormatting.BOLD));
        content.add(Component.translatable("journal.faded_pearl.basic.care_controls"));
        content.add(Component.empty());
        content.add(Component.translatable("journal.faded_pearl.basic.name").withStyle(ChatFormatting.BOLD));
        content.add(snapshot.knownName().isEmpty() ? unknown() : Component.literal(snapshot.knownName()));
        content.add(Component.empty());
        content.add(Component.translatable("journal.faded_pearl.basic.healing_color").withStyle(ChatFormatting.BOLD));
        String color = String.format(Locale.ROOT, "#%06X", snapshot.healingColor());
        content.add(Component.literal(color).withStyle(style -> style.withColor(TextColor.fromRgb(snapshot.healingColor()))));
        content.add(Component.empty());
        content.add(Component.translatable("journal.faded_pearl.basic.origin").withStyle(ChatFormatting.BOLD));
        if (snapshot.fadeOriginKnown()) {
            content.add(Component.translatable("journal.faded_pearl.basic.origin.fade")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            content.add(Component.translatable("journal.faded_pearl.basic.origin.fade_story"));
        } else content.add(unknown());
    }

    private void addEntry(List<Component> content, boolean unlocked, String baseKey) {
        content.add(Component.empty());
        if (!unlocked) {
            content.add(unknown().copy().withStyle(ChatFormatting.BOLD));
            return;
        }
        content.add(Component.translatable(baseKey + ".title").withStyle(ChatFormatting.BOLD));
        content.add(Component.translatable(baseKey + ".text"));
    }

    private List<List<FormattedCharSequence>> paginate(List<Component> content) {
        List<List<FormattedCharSequence>> result = new ArrayList<>();
        List<FormattedCharSequence> current = new ArrayList<>();
        for (FormattedText paragraph : content) {
            List<FormattedCharSequence> wrapped = font.split(paragraph, TEXT_WIDTH);
            if (wrapped.isEmpty()) wrapped = List.of(FormattedCharSequence.EMPTY);
            for (FormattedCharSequence line : wrapped) {
                if (current.size() >= LINES_PER_PAGE) {
                    result.add(List.copyOf(current));
                    current.clear();
                }
                current.add(line);
            }
        }
        if (!current.isEmpty()) result.add(List.copyOf(current));
        return result.isEmpty() ? List.of(List.of()) : List.copyOf(result);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        if (twoPage) {
            renderSpread(graphics);
        } else {
            graphics.blit(BOOK_TEXTURE, bookLeft, bookTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
            renderPageText(graphics, pages.get(page), bookLeft + 36, bookTop + 18);
            renderPageNumber(graphics, page, bookLeft + BOOK_WIDTH / 2, bookTop + 146);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSpread(GuiGraphics graphics) {
        graphics.blit(BOOK_SPREAD_TEXTURE, bookLeft, bookTop, 0, 0,
                SPREAD_WIDTH, PAGE_HEIGHT, SPREAD_WIDTH, PAGE_HEIGHT);

        renderPageText(graphics, pages.get(page), bookLeft + 15, bookTop + 17);
        renderPageNumber(graphics, page, bookLeft + PAGE_WIDTH / 2, bookTop + 151);
        if (page + 1 < pages.size()) {
            renderPageText(graphics, pages.get(page + 1), bookLeft + PAGE_WIDTH + 15, bookTop + 17);
            renderPageNumber(graphics, page + 1, bookLeft + PAGE_WIDTH + PAGE_WIDTH / 2, bookTop + 151);
        }
    }

    private void renderPageText(GuiGraphics graphics, List<FormattedCharSequence> lines, int x, int y) {
        for (int index = 0; index < lines.size(); index++)
            graphics.drawString(font, lines.get(index), x, y + index * 10, 0x2B1B13, false);
    }

    private void renderPageNumber(GuiGraphics graphics, int pageIndex, int x, int y) {
        graphics.drawCenteredString(font, Component.translatable("screen.faded_pearl.journal.page",
                pageIndex + 1, pages.size()), x, y, 0x2B1B13);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            changePage(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component unknown() {
        return Component.translatable("journal.faded_pearl.unknown").withStyle(ChatFormatting.GRAY);
    }

    private static String key(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
