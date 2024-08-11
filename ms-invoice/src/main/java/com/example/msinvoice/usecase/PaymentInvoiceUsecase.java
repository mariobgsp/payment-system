package com.example.msinvoice.usecase;

import com.example.msinvoice.exception.definition.CommonException;
import com.example.msinvoice.exception.definition.TrxNotFoundException;
import com.example.msinvoice.model.repository.ProductTrx;
import com.example.msinvoice.model.rqrs.request.RequestInfo;
import com.example.msinvoice.model.rqrs.response.Response;
import com.example.msinvoice.model.rqrs.response.ResponseInfo;
import com.example.msinvoice.repository.ProductTrxRepository;
import com.example.msinvoice.utils.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static com.example.msinvoice.utils.CommonUtils.*;

@Component
@Slf4j
public class PaymentInvoiceUsecase extends BaseUsecase{

    @Autowired
    private ProductTrxRepository productTrxRepository;

    public void process(RequestInfo requestInfo, String transactionId){
        log.info("process payment notify {}", transactionId);
        ResponseInfo<Object> rs = new ResponseInfo<>();

        try{
            // Check by transactionId
            Optional<ProductTrx> productTrx = productTrxRepository.findByTransactionId(transactionId);
            if(productTrx.isEmpty()){
                throw new TrxNotFoundException("01","transaction not found");
            }
            ProductTrx p = productTrx.get();


            // generate document using jrxml
            log.info("generatePaymentReport... {}", transactionId);
//            generatePaymentReport(transactionId, p.getPrice().doubleValue(), p.getProductName(), p.getPaymentDate());

            p.setSysUpdateDate(new Date());
            p.setOrderStatus("SUCCESS");
            productTrxRepository.save(p);

            rs = ResponseUtils.generateSuccessRs(requestInfo);
        }catch (Exception e){
            log.error("[{} - notifyPayment][{}][{}][Error: {}]", requestInfo.getRequestId(), requestInfo.getOpName(), requestInfo.getRequestData(), e.getMessage());
            CommonException ex = (e instanceof CommonException) ? (CommonException) e : new CommonException(e);
            rs = ResponseUtils.generateException(requestInfo, ex);
        }
        log.info("done process payment notify {}", transactionId);
        super.publish(requestInfo, rs);
    }

    public static void generatePaymentReport(String transactionId, Double price, String productName, Date paymentDate) {
        try {
            // Load the .jrxml file
            File file = new File("/app/resources/templates/payment_notes.jrxml");

            JasperDesign jasperDesign = JRXmlLoader.load(file);

            // Compile the report
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // Set up the parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("transactionId", transactionId);
            parameters.put("price", "Rp "+decimalToFormat(price));
            parameters.put("productName", productName);
            parameters.put("paymentDate", convertDateToString(paymentDate));

            // Fill the report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // Export to PDF
            log.info("exporting generatePaymentReport... {}", transactionId);
            String outputFile = "/app/invoice/"+"invoice_"+ transactionId + ".pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, outputFile);

            log.info("success generate report {}", outputFile);
        } catch (JRException e) {
            log.error("error generate report message {}", e.getMessage());
        }
    }

}
