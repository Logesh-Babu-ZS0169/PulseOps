package com.intics.metrics.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.to}")
    private String toEmail;

    /**
     * Sends a combined metrics email with professional HTML formatting.
     */
    public void sendCombinedMetricsEmail(String documentType, int newRowCount, String hourRange, String metricsDetails) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[EXTERNAL] Inbound Ingestion Metrics - " + documentType);

            // Build the HTML body
            String htmlBody = "<html><body style='font-family: Arial, sans-serif; color:#333;'>"
                    + "<h2 style='color:#2E86C1;'>Inbound Ingestion Metrics Notification</h2>"
                    + "<p>Hello,</p>"
                    + "<p>This is an automated notification from the <b>Inbound Ingestion Metrics System</b>.</p>"

                    + "<h3 style='color:#28B463;'>New Hourly Data Alert</h3>"
                    + "<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse; width:80%;'>"
                    + "<tr style='background-color:#D6EAF8; text-align:left;'>"
                    + "<th>Document Type</th><th>Hour Range</th><th>New Rows Detected</th></tr>"
                    + "<tr><td>" + documentType + "</td><td>" + hourRange + "</td><td>" + newRowCount + "</td></tr>"
                    + "</table>"

                    + "<h3 style='color:#F39C12; margin-top:25px;'>Detailed Metrics</h3>"
                    + "<table border='1' cellpadding='8' cellspacing='0' "
                    + "style='border-collapse:collapse; width:80%; background-color:#FEF9E7;'>"
                    + "<tr style='background-color:#FAD7A0; text-align:left;'>"
                    + "<th>Metric</th><th>Value</th></tr>"
                    + convertMetricsToHtmlRows(metricsDetails)
                    + "</table>"

                    + "<p style='margin-top:25px;'>Please check the "
                    + "<b>Inbound Ingestion Metrics Dashboard</b> for more details.</p>"
                    + "<p>Regards,<br/><b>Inbound Ingestion Metrics Team</b></p>"
                    + "</body></html>";

            helper.setText(htmlBody, true); // Enable HTML content
            mailSender.send(mimeMessage);

            logger.info("✅ Combined metrics email sent successfully to {} for {}", toEmail, documentType);

        } catch (MessagingException e) {
            logger.error("❌ Failed to send combined metrics email for {}: {}", documentType, e.getMessage(), e);
        }
    }

    /**
     * Converts a plain-text metrics section into an HTML table format dynamically.
     * Example input:
     *
     * Hour: 2025-10-29 12:00:00.0
     * Total Documents: 68
     * Staged: 0
     * In Progress: 0
     * Completed: 68
     * Failed: 0
     * Failed in Ingestion: 0
     * Aborted: 0
     */
    private String convertMetricsToHtmlRows(String metricsDetails) {
        StringBuilder rows = new StringBuilder();
        String[] lines = metricsDetails.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("=") || line.startsWith("-")) {
                continue; // Skip separators
            }
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";

                // Highlight "Failed" metrics in red if value > 0
                String bgColor = "";
                if (key.toLowerCase().contains("failed") || key.toLowerCase().contains("aborted")) {
                    try {
                        int val = Integer.parseInt(value);
                        if (val > 0) {
                            bgColor = " style='background-color:#F5B7B1; font-weight:bold;'";
                        }
                    } catch (NumberFormatException ignored) {}
                }

                rows.append("<tr").append(bgColor).append(">")
                        .append("<td>").append(escapeHtml(key)).append("</td>")
                        .append("<td>").append(escapeHtml(value)).append("</td>")
                        .append("</tr>");
            }
        }
        return rows.toString();
    }

    /**
     * Escapes special HTML characters for safety.
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
