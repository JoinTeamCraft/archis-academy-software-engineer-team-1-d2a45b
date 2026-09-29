package tech.lokum.parkinglot.notification.service;

import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Service for building consistent, structured plain-text and HTML notification email templates.
 */
@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /**
     * Builds registration confirmation email with account details.
     */
    public EmailDetails buildUserRegistrationEmail(User user) {
        String recipient = user.getEmail();
        String name = (user.getFullName() != null && !user.getFullName().isBlank()) ? user.getFullName() : (user.getUsername() != null ? user.getUsername() : "Valued Member");
        String username = user.getUsername() != null ? user.getUsername() : user.getEmail();
        String role = user.getRole() != null ? user.getRole().name() : "USER";

        String subject = "Welcome to Parking Lot System - Registration Confirmed";

        String textBody = String.format("""
            Hello %s,

            Welcome to the Parking Lot Management System!
            Your account has been successfully created.

            --- ACCOUNT DETAILS ---
            Username: %s
            Email:    %s
            Role:     %s

            You can now log in and reserve parking spaces seamlessly across our smart lots.
            If you did not initiate this registration, please contact our support team immediately.

            Best regards,
            The Parking Lot System Team
            """, name, username, recipient, role);

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                .header { background: #1e40af; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 22px; }
                .content { padding: 24px; }
                .card { background: #f1f5f9; border-radius: 6px; padding: 16px; margin: 16px 0; }
                .row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e2e8f0; }
                .row:last-child { border-bottom: none; }
                .label { font-weight: 600; color: #475569; }
                .value { color: #0f172a; }
                .footer { background: #f8fafc; text-align: center; padding: 16px; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Welcome to Parking Lot System</h1>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>Your registration is complete! You can now easily search, reserve, and manage parking spots.</p>
                  <div class="card">
                    <div class="row"><span class="label">Username:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Email:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Role:</span><span class="value">%s</span></div>
                  </div>
                  <p>Thank you for choosing our service.</p>
                </div>
                <div class="footer">
                  <p>&copy; Parking Lot System. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """, name, username, recipient, role);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }

    /**
     * Builds booking confirmation email with reservation and spot details.
     */
    public EmailDetails buildBookingConfirmationEmail(Reservation reservation) {
        String recipient = reservation.getUser().getEmail();
        String name = reservation.getUser().getFullName() != null && !reservation.getUser().getFullName().isBlank()
            ? reservation.getUser().getFullName()
            : (reservation.getUser().getUsername() != null ? reservation.getUser().getUsername() : "Valued Customer");

        String lotName = reservation.getParkingSpot() != null && reservation.getParkingSpot().getParkingLot() != null
            ? reservation.getParkingSpot().getParkingLot().getName()
            : "Reserved Lot";
        String spotNumber = reservation.getParkingSpot() != null ? reservation.getParkingSpot().getSpotNumber() : "N/A";
        String licensePlate = reservation.getVehicle() != null ? reservation.getVehicle().getLicensePlate() : "N/A";
        String startStr = reservation.getStartTime() != null ? DATE_FORMATTER.format(reservation.getStartTime()) : "N/A";
        String endStr = reservation.getEndTime() != null ? DATE_FORMATTER.format(reservation.getEndTime()) : "N/A";
        String amountStr = reservation.getTotalAmount() != null ? String.format("$%.2f", reservation.getTotalAmount()) : "$0.00";
        String status = reservation.getStatus() != null ? reservation.getStatus().name() : "CONFIRMED";

        String subject = String.format("Booking Confirmed - Reservation #%d", reservation.getId());

        String textBody = String.format("""
            Hello %s,

            Your parking reservation has been confirmed!

            --- RESERVATION DETAILS ---
            Reservation ID: #%d
            Status:         %s
            Parking Lot:    %s
            Spot Number:    %s
            Vehicle Plate:  %s
            Start Time:     %s
            End Time:       %s
            Total Amount:   %s

            Please ensure you park only in spot %s during your reserved slot.

            Best regards,
            The Parking Lot System Team
            """, name, reservation.getId(), status, lotName, spotNumber, licensePlate, startStr, endStr, amountStr, spotNumber);

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                .header { background: #059669; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 22px; }
                .content { padding: 24px; }
                .card { background: #f1f5f9; border-radius: 6px; padding: 16px; margin: 16px 0; }
                .row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e2e8f0; }
                .row:last-child { border-bottom: none; }
                .label { font-weight: 600; color: #475569; }
                .value { color: #0f172a; }
                .total { font-weight: 700; color: #059669; }
                .footer { background: #f8fafc; text-align: center; padding: 16px; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Reservation Confirmed</h1>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>Your parking space has been successfully booked. Here are your booking details:</p>
                  <div class="card">
                    <div class="row"><span class="label">Reservation ID:</span><span class="value">#%d</span></div>
                    <div class="row"><span class="label">Status:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Parking Lot:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Spot Number:</span><span class="value"><strong>%s</strong></span></div>
                    <div class="row"><span class="label">Vehicle Plate:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Start Time:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">End Time:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Total Price:</span><span class="value total">%s</span></div>
                  </div>
                  <p>Please arrive on time and park in assigned spot <strong>%s</strong>.</p>
                </div>
                <div class="footer">
                  <p>&copy; Parking Lot System. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """, name, reservation.getId(), status, lotName, spotNumber, licensePlate, startStr, endStr, amountStr, spotNumber);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }

    /**
     * Builds payment receipt email with transaction and fee details.
     */
    public EmailDetails buildPaymentReceiptEmail(Payment payment) {
        Reservation reservation = payment.getReservation();
        String recipient = (reservation != null && reservation.getUser() != null)
            ? reservation.getUser().getEmail()
            : "customer@example.com";
        String name = (reservation != null && reservation.getUser() != null && reservation.getUser().getFullName() != null && !reservation.getUser().getFullName().isBlank())
            ? reservation.getUser().getFullName()
            : "Valued Customer";

        Long reservationId = reservation != null ? reservation.getId() : null;
        String txId = payment.getTransactionId() != null ? payment.getTransactionId() : "N/A";
        String amountStr = payment.getAmount() != null ? String.format("$%.2f %s", payment.getAmount(), payment.getCurrency() != null ? payment.getCurrency() : "USD") : "$0.00 USD";
        String method = payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "STANDARD";
        String status = payment.getStatus() != null ? payment.getStatus().name() : "SUCCESS";
        String dateStr = payment.getPaidAt() != null ? DATE_FORMATTER.format(payment.getPaidAt()) : DATE_FORMATTER.format(java.time.Instant.now());

        String subject = String.format("Payment Receipt - Transaction #%s", txId);

        String textBody = String.format("""
            Hello %s,

            Thank you for your payment! Here is your official payment receipt.

            --- RECEIPT SUMMARY ---
            Payment ID:      #%d
            Transaction ID:  %s
            Reservation ID:  #%s
            Amount Paid:     %s
            Payment Method:  %s
            Payment Status:  %s
            Date & Time:     %s

            This receipt confirms that full payment was processed successfully.

            Best regards,
            The Parking Lot System Team
            """, name, payment.getId(), txId, reservationId != null ? reservationId.toString() : "N/A", amountStr, method, status, dateStr);

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
                .header { background: #4338ca; color: #ffffff; padding: 24px; text-align: center; }
                .header h1 { margin: 0; font-size: 22px; }
                .content { padding: 24px; }
                .card { background: #f1f5f9; border-radius: 6px; padding: 16px; margin: 16px 0; }
                .row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e2e8f0; }
                .row:last-child { border-bottom: none; }
                .label { font-weight: 600; color: #475569; }
                .value { color: #0f172a; }
                .amount { font-weight: 700; color: #4338ca; }
                .footer { background: #f8fafc; text-align: center; padding: 16px; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Payment Receipt</h1>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>Your payment has been successfully processed. Here is your transaction receipt:</p>
                  <div class="card">
                    <div class="row"><span class="label">Payment ID:</span><span class="value">#%d</span></div>
                    <div class="row"><span class="label">Transaction ID:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Reservation ID:</span><span class="value">#%s</span></div>
                    <div class="row"><span class="label">Amount Paid:</span><span class="value amount">%s</span></div>
                    <div class="row"><span class="label">Method:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Status:</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Date:</span><span class="value">%s</span></div>
                  </div>
                  <p>Thank you for your business!</p>
                </div>
                <div class="footer">
                  <p>&copy; Parking Lot System. All rights reserved.</p>
                </div>
              </div>
            </body>
            </html>
            """, name, payment.getId(), txId, reservationId != null ? reservationId.toString() : "N/A", amountStr, method, status, dateStr);

        return new EmailDetails(recipient, subject, textBody, htmlBody);
    }
}
