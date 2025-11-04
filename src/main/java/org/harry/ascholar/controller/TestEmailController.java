//package org.harry.ascholar.controller;
//
//import org.harry.ascholar.service.EmailService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//    @RestController
//    @RequestMapping("/api/test")
//    public class TestEmailController {
//
//        @Autowired
//        private EmailService emailService;
//
//        @GetMapping("/email-config")
//        public ResponseEntity<String> testEmailConfig() {
//            try {
//                boolean isWorking = emailService.testEmailConfiguration();
//                if (isWorking) {
//                    return ResponseEntity.ok("✅ Email configuration is working!");
//                } else {
//                    return ResponseEntity.ok("⚠️ Email is in development mode (no real emails sent)");
//                }
//            } catch (Exception e) {
//                return ResponseEntity.badRequest().body("❌ Email configuration error: " + e.getMessage());
//            }
//        }
//    }
//}
