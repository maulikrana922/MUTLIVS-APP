package com.es.multivs.data.bledevices.multivs;

import android.app.Activity;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GraphHandler {

    public static final int SAMPLES_IN_VIEW = 1200;
    public static final int SPLIT_SERIES_POSITION = 1100;
    private static final int SAMPLE_TIME_INTERVAL_IN_MILLIS = 5;
    private static final int SAMPLES_TO_UPDATE_AT_EACH_ITERATION = 5;

    private LineGraphSeries<DataPoint> ecgSeriesRight;
    private LineGraphSeries<DataPoint> ecgSeriesLeft;
    private PointsGraphSeries<DataPoint> ecgSeriesEnd;
    private LineGraphSeries<DataPoint> ppgSeriesRight;
    private LineGraphSeries<DataPoint> ppgSeriesLeft;
    private PointsGraphSeries<DataPoint> ppgSeriesEnd;

    private int ecgSampleNumber = 0;
    private int ppgSampleNumber = 0;

    private Queue<Integer> ecgSamplesQueue;
    private Queue<Integer> ppgSamplesQueue;

    private List<DataPoint> totalEcgDataPoints;
    private List<DataPoint> ecgDataPointsRight;
    private List<DataPoint> ecgDataPointsLeft;

    private List<DataPoint> totalPpgDataPoints;
    private List<DataPoint> ppgDataPointsRight;
    private List<DataPoint> ppgDataPointsLeft;

    private boolean isEcgLeftGraphVisible;
    private boolean isEcgRightGraphVisible;

    private boolean isPpgLeftGraphVisible;
    private boolean isPpgRightGraphVisible;

    private GraphView graphEcg;
    private GraphView graphPpg;

    private boolean keepRunning;

    boolean isCountdownStarted;

    private int secondsLeft = 0;
    private boolean startedReadingFromQueue;

//    private WeakReference<Activity> activityRef;
    private Activity activityRef;

    public GraphHandler(Activity activity, GraphView ecgGraph, GraphView graphPpg) {

//        this.activityRef = new WeakReference<>((Activity) context);
        this.activityRef = activity;
        this.graphEcg = ecgGraph;
        this.graphPpg = graphPpg;

        ecgSamplesQueue = new LinkedList<>();
        ppgSamplesQueue = new LinkedList<>();

        clearGraph();
        configureGraphs();
        initFlags();
    }

    private void clearGraph() {
        if (graphEcg != null) {
            graphEcg.removeAllSeries();
            graphPpg.removeAllSeries();
        }
    }

    private void configureGraphs() {

        ecgSeriesRight = new LineGraphSeries<>();
        ecgSeriesLeft = new LineGraphSeries<>();
        ppgSeriesRight = new LineGraphSeries<>();
        ppgSeriesLeft = new LineGraphSeries<>();

        ecgSeriesRight.setThickness(4);
        ecgSeriesLeft.setThickness(4);
        ppgSeriesRight.setThickness(4);
        ppgSeriesLeft.setThickness(4);

        int redColor = Color.parseColor("#ffff726f");
//        int whiteColor = Color.parseColor("#7FFFFFFF");

        ecgSeriesRight.setColor(redColor);
        ecgSeriesLeft.setColor(redColor);
        ecgSeriesEnd = new PointsGraphSeries<>();
        ecgSeriesEnd.setColor(Color.WHITE);
        ecgSeriesEnd.setSize(4);

        ppgSeriesRight.setColor(Color.CYAN);
        ppgSeriesLeft.setColor(Color.CYAN);
        ppgSeriesEnd = new PointsGraphSeries<>();
        ppgSeriesEnd.setColor(Color.WHITE);
        ppgSeriesEnd.setSize(4);

        graphEcg.addSeries(ecgSeriesRight);
        graphEcg.addSeries(ecgSeriesLeft);
        graphEcg.addSeries(ecgSeriesEnd);

        graphPpg.addSeries(ppgSeriesRight);
        graphPpg.addSeries(ppgSeriesLeft);
        graphPpg.addSeries(ppgSeriesEnd);

        GridLabelRenderer glrEcg = graphEcg.getGridLabelRenderer();
        glrEcg.setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        graphEcg.getViewport().setYAxisBoundsManual(true);
        graphEcg.getViewport().setXAxisBoundsManual(true);
        graphEcg.getViewport().setMinX(0);
        graphEcg.getViewport().setMaxX(SAMPLES_IN_VIEW);
        graphEcg.getViewport().setMinY(-1.5);
        graphEcg.getViewport().setMaxY(1.5);

        GridLabelRenderer glrPpg = graphPpg.getGridLabelRenderer();
        glrPpg.setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        graphPpg.getViewport().setYAxisBoundsManual(true);
        graphPpg.getViewport().setXAxisBoundsManual(true);
        graphPpg.getViewport().setMinY(-1.5);
        graphPpg.getViewport().setMaxY(1.5);
        graphPpg.getViewport().setMinX(0);
        graphPpg.getViewport().setMaxX(SAMPLES_IN_VIEW);

        glrEcg.setVerticalLabelsVisible(true);
        glrEcg.setHorizontalLabelsVisible(true);
        glrEcg.setVerticalLabelsColor(Color.BLACK);
        glrEcg.setHorizontalLabelsColor(Color.BLACK);
        glrEcg.setPadding(8);
        glrEcg.setNumHorizontalLabels(7);
        glrEcg.setNumVerticalLabels(7);
        glrEcg.setHighlightZeroLines(true);
        glrEcg.setHumanRounding(true);

        glrPpg.setVerticalLabelsVisible(true);
        glrPpg.setHorizontalLabelsVisible(true);
        glrPpg.setVerticalLabelsColor(Color.BLACK);
        glrPpg.setHorizontalLabelsColor(Color.BLACK);
        glrPpg.setPadding(8);
        glrPpg.setNumHorizontalLabels(7);
        glrPpg.setNumVerticalLabels(7);
        glrPpg.setHighlightZeroLines(true);
        glrPpg.setHumanRounding(true);

        ppgSampleNumber = 0;
    }

    /**
     * Terminates the runnable in {@link GraphHandler#startReadingFromQueue}
     * by updating {@link GraphHandler#keepRunning} to false.
     * This will end
     */
    public void stopGraph() {
        keepRunning = false;
        startedReadingFromQueue = false;
    }

    public boolean isStartedReadFromQueue() {
        return this.startedReadingFromQueue;
    }

    public synchronized void addSamplesToQueue(PatchData patchData) {
        ecgSamplesQueue.addAll(patchData.getEcgSampleList());
        ppgSamplesQueue.addAll(patchData.getPpgSampleList());
    }

    public void startReadingFromQueue(PatchData patchData) {
        keepRunning = true;
        startedReadingFromQueue = true;
        new Thread(() -> {
            while (keepRunning) {
                if (patchData.isEcg() && !ecgSamplesQueue.isEmpty()) {
                    drawEcgData();
                }
                if (patchData.isPpg() && !ppgSamplesQueue.isEmpty()) {
                    drawPpgData();
                }
                try {
                    Thread.sleep((SAMPLE_TIME_INTERVAL_IN_MILLIS - 1) * SAMPLES_TO_UPDATE_AT_EACH_ITERATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void initFlags() {

        totalEcgDataPoints = new ArrayList<>();
        ecgDataPointsLeft = new ArrayList<>();
        ecgDataPointsRight = new ArrayList<>();

        totalPpgDataPoints = new ArrayList<>();
        ppgDataPointsRight = new ArrayList<>();
        ppgDataPointsLeft = new ArrayList<>();
        ecgSamplesQueue.clear();
        ppgSamplesQueue.clear();

        isCountdownStarted = false;
        startedReadingFromQueue = false;

        isEcgLeftGraphVisible = false;
        isEcgRightGraphVisible = true;

        isPpgLeftGraphVisible = false;
        isPpgRightGraphVisible = true;
        ecgSampleNumber = 0;
    }

    @NonNull
    private synchronized Integer pollPpgSample() {
        Integer i = ppgSamplesQueue.poll();
        if (i == null) {
            return -1;
        }
        return i;
    }

    @org.jetbrains.annotations.Nullable
    public synchronized Integer pollEcgSample() {
        Integer i = ecgSamplesQueue.poll();
        if (i == null) {
            return -1;
        }
        return i;
    }

    private void createEcgFullViewPortList(int ecgSample, List<DataPoint> totalEcgDataPoints) {
        if (ecgSampleNumber >= SAMPLES_IN_VIEW)
            totalEcgDataPoints.set(
                    Math.round(ecgSampleNumber % SAMPLES_IN_VIEW),
                    new DataPoint(Math.round(ecgSampleNumber % SAMPLES_IN_VIEW),
                            getSampleValueInMilliVolts(ecgSample))
            );
        else
            totalEcgDataPoints.add(new DataPoint(Math.round(ecgSampleNumber % SAMPLES_IN_VIEW),
                    getSampleValueInMilliVolts(ecgSample)));
    }

    private void createPpgFullViewPortList(int ppgSample, List<DataPoint> totalPpgDataPoints) {
        if (ppgSampleNumber >= SAMPLES_IN_VIEW)
            totalPpgDataPoints.set(
                    Math.round(ppgSampleNumber % SAMPLES_IN_VIEW),
                    new DataPoint(Math.round(ppgSampleNumber % SAMPLES_IN_VIEW),
                            ppgSample)
            );
        else
            totalPpgDataPoints.add(new DataPoint(Math.round(ppgSampleNumber % SAMPLES_IN_VIEW), ppgSample)
            );
    }

    private double getSampleValueInMilliVolts(int s) {
        return (double) (s - 1650) / 1100;
    }

    private void onFirstEcgIterationState() {
        if (ecgSampleNumber < SPLIT_SERIES_POSITION) {
            ecgDataPointsRight = totalEcgDataPoints;
        }
        else {
            ecgDataPointsRight = totalEcgDataPoints.subList(
                    ecgSampleNumber - SPLIT_SERIES_POSITION, totalEcgDataPoints.size() - 1);
        }
        isEcgRightGraphVisible = true;
        isEcgLeftGraphVisible = false;
    }

    private void onFirstPpgIterationState() {
        if (ppgSampleNumber < SPLIT_SERIES_POSITION) {
            ppgDataPointsRight = totalPpgDataPoints;
        }
        else {
            ppgDataPointsRight = totalPpgDataPoints.subList(
                    ppgSampleNumber - SPLIT_SERIES_POSITION, totalPpgDataPoints.size() - 1);
        }
        isPpgRightGraphVisible = true;
        isPpgLeftGraphVisible = false;
    }

    private void drawPpgData() {
        if (!ppgSamplesQueue.isEmpty()) {
            int ppgSample = -1;
            for (int i = 0; i < SAMPLES_TO_UPDATE_AT_EACH_ITERATION; i++) {
                int sample = pollPpgSample();
                if (sample != -1) {
                    createPpgFullViewPortList(sample, totalPpgDataPoints);
                    ppgSampleNumber++;
                    ppgSample = sample;
                }
            }
            if (ppgSample == -1) return;

            if (ppgSampleNumber < SAMPLES_IN_VIEW) {
                onFirstPpgIterationState();
            }
            else {
                isPpgLeftGraphVisible = true;

                int startingPosition = Math
                        .round((ppgSampleNumber - SPLIT_SERIES_POSITION) % SAMPLES_IN_VIEW);

                ppgDataPointsRight = totalPpgDataPoints.subList(startingPosition, SAMPLES_IN_VIEW);

                if (Math.round((ppgSampleNumber) % SAMPLES_IN_VIEW) < SPLIT_SERIES_POSITION) {
                    graphPpg.getViewport().setYAxisBoundsManual(false);
                    ppgDataPointsLeft = totalPpgDataPoints.subList(
                            0, Math.round((ppgSampleNumber) % SAMPLES_IN_VIEW));

                    isPpgRightGraphVisible = true;
                }
                else {
                    graphPpg.getViewport().setYAxisBoundsManual(true);
                    isPpgRightGraphVisible = false;
                    ppgDataPointsLeft = totalPpgDataPoints.subList(
                            (Math.round((ppgSampleNumber) % SAMPLES_IN_VIEW)) - SPLIT_SERIES_POSITION,
                            Math.round((ppgSampleNumber) % SAMPLES_IN_VIEW));
                }
            }

            drawPpgOnMainThread(
                    ppgSample,
                    ppgDataPointsRight.toArray(new DataPoint[0]),
                    ppgDataPointsLeft.toArray(new DataPoint[0])
            );
        }
    }

    private void drawEcgData() {
        if (!ecgSamplesQueue.isEmpty()) {
            int ecgSample = -1;
            for (int i = 0; i < SAMPLES_TO_UPDATE_AT_EACH_ITERATION; i++) {
                int sample = pollEcgSample();
                if (sample != -1) {
                    createEcgFullViewPortList(sample, totalEcgDataPoints);
                    ecgSampleNumber++;
                    ecgSample = sample;
                }
            }

            if (ecgSample == -1) return;

            if (ecgSampleNumber < SAMPLES_IN_VIEW) {
                onFirstEcgIterationState();
            }
            else {
                isEcgLeftGraphVisible = true;

                int startingPosition = Math.round((ecgSampleNumber - SPLIT_SERIES_POSITION) % SAMPLES_IN_VIEW);
                ecgDataPointsRight = totalEcgDataPoints.subList(startingPosition, SAMPLES_IN_VIEW);
                if (Math.round((ecgSampleNumber) % SAMPLES_IN_VIEW) < SPLIT_SERIES_POSITION) {
                    ecgDataPointsLeft = totalEcgDataPoints.subList(0, Math.round((ecgSampleNumber) % SAMPLES_IN_VIEW));
                    isEcgRightGraphVisible = true;
                }
                else {
                    isEcgRightGraphVisible = false;

                    ecgDataPointsLeft = totalEcgDataPoints.subList(
                            (Math.round((ecgSampleNumber) % SAMPLES_IN_VIEW)) - SPLIT_SERIES_POSITION,
                            Math.round((ecgSampleNumber) % SAMPLES_IN_VIEW)
                    );
                }
            }
            drawEcgOnMainThread(
                    ecgSample,
                    ecgDataPointsRight.toArray(new DataPoint[0]),
                    ecgDataPointsLeft.toArray(new DataPoint[0])
            );
        }
    }

    private void drawEcgOnMainThread(int ecgSample, DataPoint[] dataPointsRight, DataPoint[] dataPointsLeft) {
        if (activityRef !=null/*activityRef != null*/) {
            if (isEcgRightGraphVisible) {
                ecgSeriesRight.resetData(dataPointsRight);
            }
            else {
                ecgSeriesRight.resetData(new DataPoint[]{});
            }
            if (isEcgLeftGraphVisible) {
                ecgSeriesLeft.resetData(dataPointsLeft);
            }
            else {
                ecgSeriesLeft.resetData(new DataPoint[]{});
            }
            ecgSeriesEnd.resetData(new DataPoint[]{
                    new DataPoint(Math.round(ecgSampleNumber % SAMPLES_IN_VIEW),
                            getSampleValueInMilliVolts(ecgSample))
            });
//            activityRef.get().runOnUiThread(() -> {
//                if (isEcgRightGraphVisible) {
//                    ecgSeriesRight.resetData(dataPointsRight);
//                }
//                else {
//                    ecgSeriesRight.resetData(new DataPoint[]{});
//                }
//                if (isEcgLeftGraphVisible) {
//                    ecgSeriesLeft.resetData(dataPointsLeft);
//                }
//                else {
//                    ecgSeriesLeft.resetData(new DataPoint[]{});
//                }
//
//                ecgSeriesEnd.resetData(new DataPoint[]{
//                        new DataPoint(Math.round(ecgSampleNumber % SAMPLES_IN_VIEW),
//                                getSampleValueInMilliVolts(ecgSample))
//                });
//            });
        }
    }

    private void drawPpgOnMainThread(int ppgSample, DataPoint[] dataPointsRight, DataPoint[] dataPointsLeft) {
        if (activityRef !=null/*activityRef != null*/) {
            if (isPpgRightGraphVisible) {
                ppgSeriesRight.resetData(dataPointsRight);
            }
            else {
                ppgSeriesRight.resetData(new DataPoint[]{});
            }
            if (isPpgLeftGraphVisible) {
                ppgSeriesLeft.resetData(dataPointsLeft);
            }
            else {
                ppgSeriesLeft.resetData(new DataPoint[]{});
            }
            ppgSeriesEnd.resetData(new DataPoint[]{
                    new DataPoint(Math.round(ppgSampleNumber % SAMPLES_IN_VIEW), ppgSample)
            });
//            activityRef.get().runOnUiThread(() -> {
//                if (isPpgRightGraphVisible) {
//                    ppgSeriesRight.resetData(dataPointsRight);
//                }
//                else {
//                    ppgSeriesRight.resetData(new DataPoint[]{});
//                }
//                if (isPpgLeftGraphVisible) {
//                    ppgSeriesLeft.resetData(dataPointsLeft);
//                }
//                else {
//                    ppgSeriesLeft.resetData(new DataPoint[]{});
//                }
//                ppgSeriesEnd.resetData(new DataPoint[]{
//                        new DataPoint(Math.round(ppgSampleNumber % SAMPLES_IN_VIEW), ppgSample)
//                });
//            });
        }
    }
}
