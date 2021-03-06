package com.ys.mfc;

//http://java-online.ru/swing-windows.xhtml
//frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//код для тестирования 0789873

import com.WacomGSS.STU.Protocol.EncodingMode;
import com.WacomGSS.STU.Protocol.InkingMode;
import com.WacomGSS.STU.Protocol.ProtocolHelper;
import com.WacomGSS.STU.STUException;
import com.WacomGSS.STU.Tablet;
import com.WacomGSS.STU.UsbDevice;
import com.ys.mfc.mkgu.MkguQuestionXmlIndicator;
import com.ys.mfc.mkgu.MkguQuestionXmlQuestions;
import com.ys.mfc.mkgu.MkguQuestionXmlRoot;
import com.ys.mfc.mkgu.MkguQuestionnaires;
import com.ys.mfc.util.BuyImage;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Main extends JFrame {
    public static final Logger log = LoggerFactory.getLogger(HttpAdapter.class);

    static HttpAdapter adapter = HttpAdapter.getInstance();
    private static Map mkguFormVersion;
    private static String formVersion;

    private String informString;
    private JLabel informStringLabel = new JLabel();
    private JPanel panel = new JPanel();
    private BufferedImage buyImage;
    private List<String[]> rates = new ArrayList<>();
    private String postXml;
    private String orderCode;
    private String okato = "50401000000";
    //    private String okato = "30000000000";
    private String authorityId = "123";
    private static String version;


    public Main(String orderCode) {
        this.orderCode = orderCode;
        try {
            System.loadLibrary("wgssSTU");
        } catch (UnsatisfiedLinkError e) {
            String name = "wgssSTU.dll";
            Path path = FileSystems.getDefault().getPath(".", name);

            try (InputStream input = Main.class.getResourceAsStream("/" + name)) {
                if (input == null) {
                    throw new FileNotFoundException("Не найден ресурс wgssSTU.dll");
                }
                Files.copy(input, path);
                System.loadLibrary("wgssSTU");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        this.setTitle("Оценка качеcтва оказания услуг");
        informStringLabel.setText("Начинаем...");
        this.panel.setPreferredSize(new Dimension(400, 200));
        this.setBounds(100,100, 400, 200);
        this.panel.add(informStringLabel);
        this.add(this.panel, BorderLayout.NORTH);
        this.pack();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public static void main(String[] args) throws IOException {
//        runQuest(orderCode);
        String orderCode = "";
//        JFrame.setDefaultLookAndFeelDecorated(true);

        JFrame codeFrame = new JFrame();
//        codeFrame.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);

        codeFrame.setTitle("Код заявления:");
        codeFrame.setBounds(10, 10, 200, 60);
        codeFrame.setAlwaysOnTop(true);
        codeFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        codeFrame.setResizable(false);

        //Поле, чтобы фокус при открытии ловить в него
        JTextField zeroField = new JTextField();
        zeroField.setMaximumSize(new Dimension(1, 1));
        codeFrame.getContentPane().add(zeroField);

        //Трэш для обхода непонятного исключения
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            DataFlavor[] dataFlavors = clipboard.getAvailableDataFlavors();
        } catch (Exception ex) {
        }

        JTextField codeField = new JTextField();
        codeField.setHorizontalAlignment(0);

        codeField.setHorizontalAlignment(0);
        codeField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                codeField.setText("");
                codeField.update(codeField.getGraphics());
            }

            @Override
            public void focusLost(FocusEvent e) {
                codeField.setText("");
            }
        });

        codeField.addActionListener(e -> runQuest(codeField.getText()));

        codeFrame.getContentPane().add(codeField);
        codeFrame.setVisible(true);
    }

    public static void runQuest(String orderCode) {

        Main mainFrame = new Main(orderCode);
//        String orderCode = JOptionPane.showInputDialog("Введите код заявления");

        try {
            com.WacomGSS.STU.UsbDevice[] usbDevices = UsbDevice.getUsbDevices();
            if (usbDevices != null && usbDevices.length > 0) {

                mkguFormVersion = adapter.getMkguFormVersion(orderCode);
                if (("ALREADY_FILLED".equals(mkguFormVersion.get("status")))) {
                    JOptionPane.showMessageDialog(null, "Оценка заявления с кодом " + orderCode + " уже была произведена.");
//                    System.exit(0);
                    return;
                } else if (!"OK".equals(mkguFormVersion.get("status"))) {
                    JOptionPane.showMessageDialog(null, "Заявление с кодом " + orderCode + " не найдено");
//                    System.exit(0);
                    return;
                }
                MkguQuestionXmlRoot questions = null;
                try {
                    questions = getQuestions(mkguFormVersion, orderCode);
                } catch (java.lang.Exception e) {
                    JOptionPane.showMessageDialog(null, "Не найдено заявление с кодом: " + orderCode);
//                    System.exit(0);
                    return;
                }
                mainFrame.setVisible(true);
                EstimationQuestionForm estimationQuestionForm = null;
                for (MkguQuestionXmlIndicator estimationQuestion : questions.getIndicator()) {
                    List<AnswerVariant> answerVariants = new ArrayList<>();
                    List<MkguQuestionXmlQuestions> stepAnswerVariants = estimationQuestion.getIndicator();

                    stepAnswerVariants.forEach(variant ->
                            answerVariants.add(new AnswerVariant(
                                    variant.getQuestionValue(),
                                    variant.getQuestionText(),
                                    variant.getAltTitle()))
                    );
                    estimationQuestionForm = new EstimationQuestionForm(
                            usbDevices[0],
                            estimationQuestion.getIndicatorId(),
                            estimationQuestion.getIndicatorId(),
                            estimationQuestion.getQuestionTitle(),
                            estimationQuestion.getDescriptionTitle(),
                            answerVariants);

                    while (estimationQuestionForm.getPressedButtonId() == null) {
                        Thread.sleep(100);
                    }
                    mainFrame.informStringLabel.setText("Ответ = " + estimationQuestionForm.getPressedButtonId());
                    mainFrame.rates.add(new String[]{estimationQuestion.getIndicatorId(),
                            estimationQuestionForm.getPressedButtonId()});
                    estimationQuestionForm.dispose();
                }

                int status = adapter.postQuestions(version, orderCode, mainFrame.getRatesString(mainFrame.rates));

                mainFrame.informStringLabel.setText("Оценка завершена. Статус = " + status);


                if (estimationQuestionForm != null) {
                    BufferedImage buyImage = new BuyImage(estimationQuestionForm.getCapability()).getBitmap();
                    byte[] bitmapData = ProtocolHelper.flatten(buyImage,
                            buyImage.getWidth(), buyImage.getHeight(),
                            true);

                    // Add the delagate that receives pen data.
                    Tablet tablet = new Tablet();
                    tablet.usbConnect(usbDevices[0], true);
                    tablet.addTabletHandler(estimationQuestionForm);

                    // Enable the pen data on the screen (if not already)
                    tablet.setInkingMode(InkingMode.Off);
                    tablet.writeImage(EncodingMode.EncodingMode_16bit_Bulk, bitmapData);


                    Thread.sleep(10000);
                    tablet.setClearScreen();
                    tablet.disconnect();
                }


                return;

            } else {
                throw new RuntimeException("No USB tablets attached");
            }
        } catch (STUException e) {
            JOptionPane.showMessageDialog(null, e.toString());
            e.printStackTrace();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, e.toString());
            e.printStackTrace();
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(null, e.toString());
            e.printStackTrace();
        }
    }


    private String getRatesString(List<String[]> rates) {

        StringBuilder stringBuilder = new StringBuilder();
        rates.forEach(rate -> {
            stringBuilder.append("<rate indicator-id=\"");
            stringBuilder.append(rate[0]);
            stringBuilder.append("\" value-id=\"");
            stringBuilder.append(rate[1]);
            stringBuilder.append("\">");
            stringBuilder.append(rate[1]);
            stringBuilder.append("</rate>");
        });
        return stringBuilder.toString();
    }

    private static MkguQuestionXmlRoot getQuestions(Map<String, String> mkguFormVersion, String orderNumber) {
        List<MkguQuestionnaires> mkguQuestionnaires = HttpAdapter.getInstance().getMkguQuestionnaires();
        MkguQuestionXmlRoot mkguQuestionXmlRoot = new MkguQuestionXmlRoot();
        mkguQuestionXmlRoot.setOrderNumber(orderNumber);
        version = mkguFormVersion.get("version");
        String xml = mkguQuestionnaires.stream()
                .filter(element -> element.getVersion().equals(mkguFormVersion.get("version")))
                .collect(Collectors.toList())
                .get(0)
                .getXml();

        xml = xml.replaceAll("\\n", "");
        xml = xml.replaceAll("\\\\", "");

        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            Document doc = saxBuilder.build(new StringReader(xml));
            List<MkguQuestionXmlIndicator> mkguQuestionXmlIndicators = new ArrayList<>();
            Element rootElement = doc.getRootElement();
            Element blocks = (Element) rootElement.getChildren("blocks").get(0);
            mkguQuestionXmlRoot.setQuestionTitle(((Element) blocks.getChildren().get(0)).getValue());
            Element indicators = (Element) rootElement.getChildren("indicators").get(0);
            Iterator iterator = indicators.getChildren().iterator();

            indicators.getChildren().forEach((Element element) -> {
                MkguQuestionXmlIndicator mkguQuestionXmlIndicator = new MkguQuestionXmlIndicator();
                mkguQuestionXmlIndicator.setIndicatorId(element.getAttribute("id").getValue());
                mkguQuestionXmlIndicator.setQuestionTitle(element.getChild("title").getValue());
                mkguQuestionXmlIndicator.setDescriptionTitle(element.getChild("description").getValue());
                List<Element> values = element.getChild("values").getChildren();
                List<MkguQuestionXmlQuestions> mkguQuestionXmlList = new ArrayList<>();

                values.forEach(element1 -> mkguQuestionXmlList.add(
                        new MkguQuestionXmlQuestions(
                                element1.getAttribute("id").getValue(),
                                element1.getChild("title").getValue(),
                                element1.getChild("alt-title").getValue())));

                mkguQuestionXmlIndicator.setIndicator(mkguQuestionXmlList);
                mkguQuestionXmlIndicators.add(mkguQuestionXmlIndicator);
            });

            mkguQuestionXmlRoot.setIndicator(mkguQuestionXmlIndicators);
            return mkguQuestionXmlRoot;

        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new MkguQuestionXmlRoot();
    }

}
