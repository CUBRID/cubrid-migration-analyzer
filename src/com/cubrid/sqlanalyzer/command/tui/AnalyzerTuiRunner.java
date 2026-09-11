/*
 * Copyright (c) 2025-2026 CUBRID Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.cubrid.sqlanalyzer.command.tui;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.cubrid.sqlanalyzer.command.model.AnalyzerSession;
import com.cubrid.sqlanalyzer.command.service.AnalyzerService;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiObjectCountPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiLayout;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiOverviewPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiProgressPage.ProgressView;
import com.cubrid.sqlanalyzer.command.tui.page.AnalyzerTuiResultPage;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerObjectCountPreviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerOverviewViewModel;
import com.cubrid.sqlanalyzer.command.viewmodel.AnalyzerResultViewModel;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzerTuiRunner {
    private static final TerminalSize DEFAULT_TERMINAL_SIZE = new TerminalSize(100, 30);
    private static final int METADATA_LOADING_TICK_MILLIS = 500;
    private static final int METADATA_LOADING_MAX_DOTS = 5;
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerTuiRunner.class);

    private final TerminalSize initialTerminalSize;
    private final AnalyzerTuiOverviewPage overviewPage;
    private final AnalyzerTuiObjectCountPage objectCountPage;
    private final AnalyzerTuiProgressPage progressPage;
    private final AnalyzerTuiResultPage resultPage;

    public AnalyzerTuiRunner() {
        this(DEFAULT_TERMINAL_SIZE);
    }

    private AnalyzerTuiRunner(TerminalSize initialTerminalSize) {
        this(
                initialTerminalSize,
                new AnalyzerTuiOverviewPage(),
                new AnalyzerTuiObjectCountPage(),
                new AnalyzerTuiProgressPage(),
                new AnalyzerTuiResultPage());
    }

    AnalyzerTuiRunner(
            TerminalSize initialTerminalSize,
            AnalyzerTuiOverviewPage overviewPage,
            AnalyzerTuiObjectCountPage objectCountPage,
            AnalyzerTuiProgressPage progressPage,
            AnalyzerTuiResultPage resultPage) {
        this.initialTerminalSize = initialTerminalSize;
        this.overviewPage = overviewPage;
        this.objectCountPage = objectCountPage;
        this.progressPage = progressPage;
        this.resultPage = resultPage;
    }

    public void start(AnalyzerSession session, AnalyzerService analyzerService) throws IOException {
        LOG.info("Starting TUI runner.");
        try (Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(initialTerminalSize)
                .createScreen()) {
            screen.startScreen();
            MultiWindowTextGUI gui = createTextGui(screen);
            BasicWindow window = new BasicWindow("CUBRID SQL Analyzer");
            applyResponsiveWindowHints(window);
            NavigationState state = new NavigationState();

            showOverview(screen, window, gui, session, analyzerService, state);
            startTerminalSizeWatcher(screen, gui, window, state);
            gui.addWindowAndWait(window);
        }
        LOG.info("TUI runner stopped.");
    }

    public void showOverview(AnalyzerOverviewViewModel overview) throws IOException {
        try (Screen screen = new DefaultTerminalFactory()
                .setInitialTerminalSize(initialTerminalSize)
                .createScreen()) {
            screen.startScreen();
            MultiWindowTextGUI gui = createTextGui(screen);
            gui.addWindowAndWait(buildOverviewWindow(screen, overview));
        }
    }

    private MultiWindowTextGUI createTextGui(Screen screen) {
        TextColor foreground = TextColor.ANSI.WHITE_BRIGHT;
        TextColor background = TextColor.ANSI.BLACK_BRIGHT;
        TextColor selectedForeground = TextColor.ANSI.BLACK;
        TextColor selectedBackground = TextColor.ANSI.WHITE;
        SimpleTheme theme = SimpleTheme.makeTheme(
                false,
                foreground,
                background,
                foreground,
                background,
                selectedForeground,
                selectedBackground,
                background);
        theme.setWindowPostRenderer(null);

        MultiWindowTextGUI gui = new MultiWindowTextGUI(
                screen,
                new DefaultWindowManager(),
                new EmptySpace(background));
        gui.setTheme(theme);
        return gui;
    }

    BasicWindow buildOverviewWindow(Screen screen, AnalyzerOverviewViewModel overview) {
        BasicWindow window = new BasicWindow("CUBRID SQL Analyzer");
        applyResponsiveWindowHints(window);
        NavigationState state = new NavigationState();
        state.currentRenderAction = () -> showOverviewWindowContent(screen, window, overview, state);
        showOverviewWindowContent(screen, window, overview, state);
        return window;
    }

    private void showOverviewWindowContent(
            Screen screen,
            BasicWindow window,
            AnalyzerOverviewViewModel overview,
            NavigationState state) {
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (showTerminalTooSmallIfNeeded(window, terminalSize, state)) {
            return;
        }
        state.terminalTooSmallVisible = false;

        Panel content = overviewPage.build(overview, terminalSize);
        content.setLayoutManager(new LinearLayout());
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    private void showOverview(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        state.currentRenderAction =
                () -> showOverview(screen, window, gui, session, analyzerService, state);
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (showTerminalTooSmallIfNeeded(
                window,
                terminalSize,
                state)) {
            return;
        }
        state.terminalTooSmallVisible = false;

        AnalyzerOverviewViewModel overview = analyzerService.getOverview(session);
        Panel content = withLayout(overviewPage.build(overview, terminalSize));
        Runnable nextAction = () -> showMetadataLoadingAndObjectCount(
                screen,
                window,
                gui,
                session,
                analyzerService,
                state);
        Button nextButton = new Button("Next", nextAction);
        content.addComponent(nextButton);
        content.addComponent(new Button("Close", window::close));
        setContent(window, content, nextButton);
    }

    private void showObjectCount(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        try {
            state.currentRenderAction =
                    () -> showObjectCount(screen, window, gui, session, analyzerService, state);
            TerminalSize terminalSize = currentTerminalSize(screen);
            if (showTerminalTooSmallIfNeeded(
                    window,
                    terminalSize,
                    state)) {
                return;
            }
            state.terminalTooSmallVisible = false;

            AnalyzerObjectCountPreviewViewModel preview = analyzerService.getObjectCountPreview(session);
            Panel content = withLayout(objectCountPage.build(preview, terminalSize));
            Runnable analyzeAction = () -> showProgressAndRun(
                    screen,
                    window,
                    gui,
                    session,
                    analyzerService,
                    state);
            Button analyzeButton = new Button("Analyze", analyzeAction);
            content.addComponent(analyzeButton);
            content.addComponent(new Button(
                    "Back",
                    () -> showOverview(screen, window, gui, session, analyzerService, state)));
            content.addComponent(new Button("Close", window::close));
            setContent(window, content, analyzeButton);
        } catch (RuntimeException ex) {
            showError(window, ex);
        }
    }

    private void showMetadataLoadingAndObjectCount(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        if (state.sourceLoaded) {
            LOG.info("Source metadata already loaded. Showing object count page.");
            showObjectCount(screen, window, gui, session, analyzerService, state);
            return;
        }

        LOG.info("Showing metadata loading page.");
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        Label loadingStatus = new Label("Loading source metadata");
        content.addComponent(loadingStatus);
        content.addComponent(new Label("The object count page will open when metadata is ready."));
        window.setComponent(content);
        AtomicBoolean loading = new AtomicBoolean(true);
        startMetadataLoadingAnimation(gui, loadingStatus, loading);

        Thread worker = new Thread(
                () -> loadMetadataAndShowObjectCount(
                        screen,
                        window,
                        gui,
                        session,
                        analyzerService,
                        state,
                        loading),
                "analyzer-tui-metadata");
        worker.setDaemon(true);
        worker.start();
    }

    private void startMetadataLoadingAnimation(
            MultiWindowTextGUI gui,
            Label loadingStatus,
            AtomicBoolean loading) {
        AtomicInteger tick = new AtomicInteger();
        Thread animator = new Thread(
                () -> {
                    while (loading.get()) {
                        int currentTick = tick.getAndIncrement();
                        String dots = ".".repeat(currentTick % (METADATA_LOADING_MAX_DOTS + 1));
                        try {
                            gui.getGUIThread().invokeLater(
                                    () -> loadingStatus.setText("Loading source metadata" + dots));
                        } catch (IllegalStateException ex) {
                            return;
                        }
                        try {
                            Thread.sleep(METADATA_LOADING_TICK_MILLIS);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                },
                "analyzer-tui-metadata-animation");
        animator.setDaemon(true);
        animator.start();
    }

    private void loadMetadataAndShowObjectCount(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state,
            AtomicBoolean loading) {
        try {
            LOG.info("Loading source metadata in TUI worker.");
            analyzerService.loadSourceCatalog(session);
            state.sourceLoaded = true;
            loading.set(false);
            gui.getGUIThread().invokeLater(
                    () -> showObjectCount(
                            screen,
                            window,
                            gui,
                            session,
                            analyzerService,
                            state));
        } catch (Throwable ex) {
            loading.set(false);
            LOG.error("Failed to load source metadata in TUI worker.", ex);
            gui.getGUIThread().invokeLater(
                    () -> showNoSourceLoadedOrError(window, session, ex));
        }
    }

    private void showNoSourceLoadedOrError(
            BasicWindow window,
            AnalyzerSession session,
            Throwable ex) {
        if (isNoAnalyzerSourceLoaded(ex)) {
            showNoSourceLoaded(window, session, ex);
            return;
        }
        showError(window, ex);
    }

    boolean isNoAnalyzerSourceLoaded(Throwable ex) {
        return ex instanceof IllegalStateException
                && AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE.equals(ex.getMessage());
    }

    private void showNoSourceLoaded(
            BasicWindow window,
            AnalyzerSession session,
            Throwable ex) {
        LOG.info("Showing TUI no source loaded page.");
        Panel content = buildNoSourceLoadedContent(session, ex);
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    Panel buildNoSourceLoadedContent(AnalyzerSession session, Throwable ex) {
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label(AnalyzerService.NO_ANALYZER_SOURCE_LOADED_MESSAGE));
        content.addComponent(new Label("No Oracle metadata or XML files were loaded."));
        if (session != null && !session.getSourceStatusMessages().isEmpty()) {
            content.addComponent(new Label(""));
            content.addComponent(new Label("Source status"));
            for (String message : session.getSourceStatusMessages()) {
                content.addComponent(new Label("  - " + message));
            }
        } else if (ex != null && ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            content.addComponent(new Label(ex.getMessage()));
        }
        return content;
    }

    private void showProgressAndRun(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            NavigationState state) {
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (showTerminalTooSmallIfNeeded(
                window,
                terminalSize,
                state)) {
            return;
        }
        state.terminalTooSmallVisible = false;

        LOG.info("Showing TUI progress page and starting analysis worker.");
        ProgressView progressView = progressPage.buildView(terminalSize);
        Panel content = withLayout(progressView.getPanel());
        window.setComponent(content);
        state.currentRenderAction = () -> restoreExistingContentIfTerminalFits(
                screen,
                window,
                content,
                state);
        Thread worker = new Thread(
                () -> runAnalysisAndShowResultButton(
                        screen,
                        window,
                        gui,
                        session,
                        analyzerService,
                        content,
                        progressView,
                        state),
                "analyzer-tui-analysis");
        worker.setDaemon(true);
        worker.start();
    }

    private void runAnalysisAndShowResultButton(
            Screen screen,
            BasicWindow window,
            MultiWindowTextGUI gui,
            AnalyzerSession session,
            AnalyzerService analyzerService,
            Panel content,
            ProgressView progressView,
            NavigationState state) {
        try {
            LOG.info("Running analysis in TUI worker.");
            analyzerService.runAnalysis(
                    session,
                    event -> gui.getGUIThread().invokeLater(() -> progressView.update(event)));
            AnalyzerResultViewModel result = analyzerService.saveResult(session);
            gui.getGUIThread().invokeLater(
                    () -> {
                        progressView.markCompleted();
                        Button resultButton = new Button(
                                "Result",
                                () -> showResult(screen, window, result, state));
                        content.addComponent(resultButton);
                        window.setFocusedInteractable(resultButton);
                    });
        } catch (Throwable ex) {
            LOG.error("Analysis failed in TUI worker.", ex);
            gui.getGUIThread().invokeLater(() -> showError(window, ex));
        }
    }

    private void showResult(
            Screen screen,
            BasicWindow window,
            AnalyzerResultViewModel result,
            NavigationState state) {
        LOG.info("Showing TUI result page.");
        state.currentRenderAction = () -> showResult(screen, window, result, state);
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (showTerminalTooSmallIfNeeded(
                window,
                terminalSize,
                state)) {
            return;
        }
        state.terminalTooSmallVisible = false;

        Panel content = withLayout(resultPage.build(result, terminalSize));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    private void showError(BasicWindow window, Throwable ex) {
        LOG.error("Showing TUI error page.", ex);
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label("Analyzer failed"));
        content.addComponent(new Label(ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        Button closeButton = new Button("Close", window::close);
        content.addComponent(closeButton);
        setContent(window, content, closeButton);
    }

    private Panel withLayout(Panel panel) {
        panel.setLayoutManager(new LinearLayout());
        return panel;
    }

    private void setContent(BasicWindow window, Panel content, Button primaryButton) {
        window.setComponent(content);
        window.setFocusedInteractable(primaryButton);
    }

    private boolean showTerminalTooSmallIfNeeded(
            BasicWindow window,
            TerminalSize terminalSize,
            NavigationState state) {
        if (!AnalyzerTuiLayout.isTooSmall(terminalSize)) {
            return false;
        }

        Runnable retryAction = state.currentRenderAction;
        state.terminalTooSmallVisible = true;
        Panel content = new Panel();
        content.setLayoutManager(new LinearLayout());
        content.addComponent(new Label("Terminal too small"));
        content.addComponent(new Label(
                "Current : "
                        + terminalSize.getColumns()
                        + " x "
                        + terminalSize.getRows()));
        content.addComponent(new Label(
                "Minimum : "
                        + AnalyzerTuiLayout.MINIMUM_TERMINAL_SIZE.getColumns()
                        + " x "
                        + AnalyzerTuiLayout.MINIMUM_TERMINAL_SIZE.getRows()));
        content.addComponent(new Label("Resize the terminal, then retry."));
        Button retryButton = new Button("Retry", retryAction);
        content.addComponent(retryButton);
        content.addComponent(new Button("Close", window::close));
        setContent(window, content, retryButton);
        return true;
    }

    private void restoreExistingContentIfTerminalFits(
            Screen screen,
            BasicWindow window,
            Panel content,
            NavigationState state) {
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (showTerminalTooSmallIfNeeded(window, terminalSize, state)) {
            return;
        }
        state.terminalTooSmallVisible = false;
        window.setComponent(content);
        window.invalidate();
    }

    private void applyResponsiveWindowHints(BasicWindow window) {
        window.setHints(List.of(
                Window.Hint.NO_DECORATIONS,
                Window.Hint.NO_POST_RENDERING,
                Window.Hint.FULL_SCREEN,
                Window.Hint.FIT_TERMINAL_WINDOW));
    }

    private TerminalSize currentTerminalSize(Screen screen) {
        TerminalSize resized = screen.doResizeIfNecessary();
        return resized == null ? screen.getTerminalSize() : resized;
    }

    private void startTerminalSizeWatcher(
            Screen screen,
            MultiWindowTextGUI gui,
            BasicWindow window,
            NavigationState state) {
        Thread watcher = new Thread(
                () -> {
                    while (!Thread.currentThread().isInterrupted() && window.isVisible()) {
                        try {
                            gui.getGUIThread().invokeLater(
                                    () -> showTerminalTooSmallOnResize(screen, window, state));
                            Thread.sleep(300L);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (IllegalStateException ex) {
                            return;
                        }
                    }
                },
                "analyzer-tui-resize");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void showTerminalTooSmallOnResize(
            Screen screen,
            BasicWindow window,
            NavigationState state) {
        TerminalSize terminalSize = currentTerminalSize(screen);
        if (AnalyzerTuiLayout.isTooSmall(terminalSize) && !state.terminalTooSmallVisible) {
            showTerminalTooSmallIfNeeded(window, terminalSize, state);
        }
    }

    private static class NavigationState {
        private boolean sourceLoaded;
        private boolean terminalTooSmallVisible;
        private Runnable currentRenderAction;
    }
}
