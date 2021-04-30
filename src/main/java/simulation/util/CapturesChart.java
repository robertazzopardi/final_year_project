package simulation.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Ellipse2D;
import java.util.List;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class CapturesChart<T> extends JFrame {

    public CapturesChart(final String title, final String name, final int episode,
            final List<T> data) {
        super(title);
        final JFreeChart chart = createChart(createDataset(name, episode, data), name);
        final ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new Dimension(700, 500));
        setContentPane(panel);
        pack();
        setVisible(true);
    }

    private XYDataset createDataset(final String name, final int episode, final List<T> data) {
        final XYSeries s1 = new XYSeries(name);
        final XYSeries s2 = new XYSeries(name + " average");

        for (int i = 0; i < episode; i++) {
            s1.add(i, (Number) data.get(i));
            s2.add(i, (Number) data.subList(0, i).stream()
                    .mapToDouble(j -> Float.valueOf(String.valueOf(j))).average().orElse(0));
        }

        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);
        return dataset;
    }

    private JFreeChart createChart(final XYDataset dataset, final String name) {
        final JFreeChart chart =
                ChartFactory.createXYLineChart(name + " per episode", "Episode", name, dataset);
        chart.removeLegend();
        final XYPlot plot = (XYPlot) chart.getPlot();
        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0));
        renderer.setSeriesFillPaint(0, Color.WHITE);
        renderer.setUseFillPaint(true);
        final NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        return chart;
    }

    public static void makeChart(final int episode, final List<Double> episodeRewards,
            final List<Integer> steps) {
        new CapturesChart<Integer>(episode + " Episodes", "Steps", episode, steps);
        new CapturesChart<Double>(episode + " Episodes", "Rewards", episode, episodeRewards);

    }
}
