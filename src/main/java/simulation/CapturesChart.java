package simulation;

// import java.awt.EventQueue;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.text.SimpleDateFormat;
// import javax.swing.JFrame;
import javax.swing.JPanel;
// import javax.swing.Timer;
// import org.jfree.chart.ChartFactory;
// import org.jfree.chart.ChartPanel;
// import org.jfree.chart.JFreeChart;
// import org.jfree.chart.axis.DateAxis;
// import org.jfree.chart.axis.ValueAxis;
// import org.jfree.chart.plot.XYPlot;
// import org.jfree.data.category.DefaultCategoryDataset;
// import org.jfree.data.time.DynamicTimeSeriesCollection;
// import org.jfree.data.time.Minute;
// import org.jfree.data.time.RegularTimePeriod;
// import org.jfree.data.time.Second;

/**
 * TODO: just make a normal xy chart after all episodes have run
 *
 * @see https://stackoverflow.com/a/21307289/230513
 */
public class CapturesChart extends JPanel {
    private static final long serialVersionUID = 1L;

    // private final DynamicTimeSeriesCollection dataset;
    // private final JFreeChart chart;

    // public CapturesChart(final String title) {
    // dataset = new DynamicTimeSeriesCollection(1, 100, new Second());
    // // dataset.setTimeBase(new Second());
    // dataset.setTimeBase(new Second());


    // dataset.setPosition(0);
    // dataset.addSeries(new Boolean[1], 0, title);
    // chart = ChartFactory.createTimeSeriesChart(title, "Episode", title, dataset, true, true,
    // false);

    // final XYPlot plot = chart.getXYPlot();
    // // final DateAxis axis = (DateAxis) plot.getDomainAxis();
    // final ValueAxis axis = plot.getDomainAxis();
    // axis.setAutoRange(true);
    // axis.setFixedAutoRange(10000);
    // // axis.setDateFormatOverride(new SimpleDateFormat("ss.SS"));
    // final ChartPanel chartPanel = new ChartPanel(chart);
    // add(chartPanel);
    // }

    // public void update(final Boolean value) {
    // System.out.println(value);
    // final Boolean[] newData = new Boolean[1];
    // newData[0] = value;
    // dataset.advanceTime();
    // dataset.appendData(newData);
    // }

    public static void startChart(final CapturesChart chart) {
        // EventQueue.invokeLater(new Runnable() {

        // @Override
        // public void run() {
        // final JFrame frame = new JFrame("testing");
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.add(chart);
        // frame.pack();
        // frame.setVisible(true);
        // }
        // });
    }
}
