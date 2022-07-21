package com.lizaapp.CreditConveyor;

import DTO.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LoanService {

    @Value("${conveyor.rate.base}")
    BigDecimal baseRate;
    @Value("${prescore.minimal.term}")
    int minimalTerm;
    @Value("${prescore.minimal.amount}")
    BigDecimal minimalAmount;
    @Value("${prescore.minimal.age}")
    long minimalAge;
    @Value("${validate.minimal.age}")
    long minimalValidateAge;
    @Value("${validate.maximum.age}")
    long maximumValidateAge;
    @Value("${validate.workExperience.total}")
    int workExperienceTotal;
    @Value("${validate.workExperience.current}")
    int workExperienceCurrent;
    @Value("${calculation.minimal.age}")
    int minimalCalculationAge;
    @Value("${calculation.maximum.age}")
    int maximumCalculationAge;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoanService.class);

    public List<LoanOfferDTO> prescore(LoanApplicationRequestDTO loanApplicationRequestDTO) throws Exception {

        //ПРЕСКОРИНГ

        String namesCheckRegex = "^[a-zA-Z]{2,30}$";
        String passportSeriesCheckRegex = "\\d{4}";
        String passportNumberCheckRegex = "\\d{6}";
        Pattern birthdatePattern = Pattern.compile("(19|20)\\d\\d-((0[1-9]|1[012])-(0[1-9]|[12]\\d)|(0[13-9]|1[012])-30|(0[13578]|1[02])-31)");
        Pattern emailPattern = Pattern.compile("[\\w.]{2,50}@[\\w.]{2,20}");

        String firstName = loanApplicationRequestDTO.getFirstName();

        String lastName = loanApplicationRequestDTO.getLastName();

        String middleName = loanApplicationRequestDTO.getMiddleName();

        String passportSeries = loanApplicationRequestDTO.getPassportSeries();

        String passportNumber = loanApplicationRequestDTO.getPassportNumber();

        LocalDate birthDate = loanApplicationRequestDTO.getBirthdate();
        Matcher birthdateMatcher = birthdatePattern.matcher(birthDate.toString());

        String email = loanApplicationRequestDTO.getEmail();
        Matcher emailMatcher = emailPattern.matcher(email);

        List<String> prescoreErrors = new ArrayList<>();

        if (!firstName.matches(namesCheckRegex)) {
            log.error("Invalid name!");
            prescoreErrors.add("Некорректное имя!");
        }

        if (!lastName.matches(namesCheckRegex)) {
            log.error("Invalid surname!");
            prescoreErrors.add("Некорректная фамилия!");
        }

        if (!middleName.isBlank() && !middleName.matches(namesCheckRegex)) {
            log.error("Invalid middle name!");
            prescoreErrors.add("Некорректное отчество!");
        }

        if (loanApplicationRequestDTO.getAmount().compareTo(minimalAmount) < 0) {
            log.error("Invalid amount! Must be greater or equal to 10000");
            prescoreErrors.add("Некорректная сумма!");
        }

        if (loanApplicationRequestDTO.getTerm() < minimalTerm) {
            log.error("Invalid term!");
            prescoreErrors.add("Некорректный срок кредита!");
        }

        if (!passportSeries.matches(passportSeriesCheckRegex)) {
            log.error("Invalid passport series!");
            prescoreErrors.add("Некорректная серия паспорта!");
        }

        if (!passportNumber.matches(passportNumberCheckRegex)) {
            log.error("Invalid passport number!");
            prescoreErrors.add("Некорректный номер паспорта!");
        }

        if (!birthdateMatcher.find()) {
            log.error("Invalid birth date!");
            prescoreErrors.add("Некорректная дата рождения!");
        }

        long fullYears = ChronoUnit.YEARS.between(loanApplicationRequestDTO.getBirthdate(), LocalDate.now());
        if (fullYears < minimalAge) {
            log.error("Age must be greater than 18!");
            prescoreErrors.add("Возраст меньше 18 лет!");
        }

        if (!emailMatcher.find()) {
            log.error("Invalid email!");
            prescoreErrors.add("Некорректный адрес электронной почты!");
        }

        if (!prescoreErrors.isEmpty()) {
            String joinedPrescoreErrors = prescoreErrors.stream()
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, joinedPrescoreErrors, null);
        }

        log.info("Creating credit offers");
        return List.of(
                makePreoffer(loanApplicationRequestDTO, false, false),
                makePreoffer(loanApplicationRequestDTO, false, true),
                makePreoffer(loanApplicationRequestDTO, true, false),
                makePreoffer(loanApplicationRequestDTO, true, true)
        );
    }

    protected LoanOfferDTO makePreoffer(
            LoanApplicationRequestDTO loanApplicationRequestDTO,
            boolean isInsuranceEnabled,
            boolean isSalaryClient) {

        //РАСЧЁТ КРЕДИТА

        log.info("Calculation of the initial conditions of the loan");

        BigDecimal insurance = loanApplicationRequestDTO.getAmount().multiply(BigDecimal.valueOf(0.01));

        BigDecimal rateWithSalary = baseRate.subtract(BigDecimal.valueOf(0.5)); //ставка с ЗП

        BigDecimal rateWithInsurance = baseRate.subtract(BigDecimal.valueOf(1.5)); //ставка со страховкой

        BigDecimal rateWithInsuranceAndSalary = baseRate.subtract(BigDecimal.valueOf(2.0)); //ставка со страховкой и ЗП

        if (isInsuranceEnabled) {
            if (isSalaryClient) {

                BigDecimal monthRate4 = rateWithInsuranceAndSalary.divide(BigDecimal.valueOf(1200), 3, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах со страховкой и ЗП
                System.out.println(monthRate4);

                BigDecimal annuityRatio4 = (monthRate4.multiply((monthRate4.add(BigDecimal.valueOf(1)))
                        .pow(loanApplicationRequestDTO.getTerm())))
                        .divide(((monthRate4.add(BigDecimal.valueOf(1)))
                                .pow(loanApplicationRequestDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING); //коэффициент аннуитета со страховкой и ЗП

                BigDecimal monthlyPayment4 = (loanApplicationRequestDTO.getAmount().add(insurance))
                        .multiply(annuityRatio4).setScale(2, RoundingMode.HALF_UP);

                return new LoanOfferDTO(
                        40L,
                        loanApplicationRequestDTO.getAmount(),
                        loanApplicationRequestDTO.getAmount().add(insurance),
                        loanApplicationRequestDTO.getTerm(),
                        monthlyPayment4,
                        rateWithInsuranceAndSalary,
                        true,
                        true
                );
            } else {

                BigDecimal monthRate3 = rateWithInsurance.divide(BigDecimal.valueOf(1200), 3, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах со страховкой

                BigDecimal annuityRatio3 = (monthRate3.multiply((monthRate3.add(BigDecimal.valueOf(1)))
                        .pow(loanApplicationRequestDTO.getTerm())))
                        .divide(((monthRate3.add(BigDecimal.valueOf(1)))
                                .pow(loanApplicationRequestDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING);//коэффициент аннуитета со страховкой или ЗП

                BigDecimal monthlyPayment3 = (loanApplicationRequestDTO.getAmount().add(insurance))
                        .multiply(annuityRatio3).setScale(2, RoundingMode.HALF_UP);

                return new LoanOfferDTO(30L,
                        loanApplicationRequestDTO.getAmount(),
                        loanApplicationRequestDTO.getAmount().add(insurance),
                        loanApplicationRequestDTO.getTerm(),
                        monthlyPayment3,
                        rateWithInsurance,
                        true,
                        false);
            }
        } else if (isSalaryClient) {

            BigDecimal monthRate2 = rateWithSalary.divide(BigDecimal.valueOf(1200), 3, RoundingMode.CEILING);//месячная ставка по кредиту не в процентах с ЗП
            System.out.println(monthRate2);

            BigDecimal annuityRatio2 = (monthRate2.multiply((monthRate2.add(BigDecimal.valueOf(1)))
                    .pow(loanApplicationRequestDTO.getTerm())))
                    .divide(((monthRate2.add(BigDecimal.valueOf(1)))
                            .pow(loanApplicationRequestDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING);//коэффициент аннуитета со страховкой или ЗП

            BigDecimal monthlyPayment2 = loanApplicationRequestDTO.getAmount()
                    .multiply(annuityRatio2).setScale(2, RoundingMode.HALF_UP);

            return new LoanOfferDTO(20L,
                    loanApplicationRequestDTO.getAmount(),
                    loanApplicationRequestDTO.getAmount(),
                    loanApplicationRequestDTO.getTerm(),
                    monthlyPayment2,
                    rateWithSalary,
                    false,
                    true);
        } else {

            BigDecimal monthRate1 = baseRate.divide(BigDecimal.valueOf(1200), 3, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах

            BigDecimal annuityRatio1 = (monthRate1.multiply((monthRate1.add(BigDecimal.valueOf(1)))
                    .pow(loanApplicationRequestDTO.getTerm())))
                    .divide(((monthRate1.add(BigDecimal.valueOf(1)))
                            .pow(loanApplicationRequestDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING);//коэффициент аннуитета без страховки и зарплаты

            BigDecimal monthlyPayment1 = loanApplicationRequestDTO.getAmount()
                    .multiply(annuityRatio1).setScale(2, RoundingMode.HALF_UP);

            return new LoanOfferDTO(10L,
                    loanApplicationRequestDTO.getAmount(),
                    loanApplicationRequestDTO.getAmount(),
                    loanApplicationRequestDTO.getTerm(),
                    monthlyPayment1,
                    baseRate,
                    false,
                    false);
        }
    }

    public void validate(ScoringDataDTO scoringDataDTO) throws Exception {

        EmploymentDTO employmentDTO = scoringDataDTO.getEmployment();

        //ПРЕСКОРИНГ

        String namesCheckRegex = "^[a-zA-Z]{2,30}$";
        String passportSeriesCheckRegex = "\\d{4}";
        String passportNumberCheckRegex = "\\d{6}";
        Pattern birthdatePattern = Pattern.compile("(19|20)\\d\\d-((0[1-9]|1[012])-(0[1-9]|[12]\\d)|(0[13-9]|1[012])-30|(0[13578]|1[02])-31)");

        String firstName = scoringDataDTO.getFirstName();

        String lastName = scoringDataDTO.getLastName();

        String middleName = scoringDataDTO.getMiddleName();

        String passportSeries = scoringDataDTO.getPassportSeries();

        String passportNumber = scoringDataDTO.getPassportNumber();

        LocalDate birthDate = scoringDataDTO.getBirthdate();
        Matcher birthdateMatcher = birthdatePattern.matcher(birthDate.toString());

        List<String> validateErrors = new ArrayList<>();

        if (!firstName.matches(namesCheckRegex)) {
            log.error("Invalid name!");
            validateErrors.add("Некорректное имя!");
        }

        if (!lastName.matches(namesCheckRegex)) {
            log.error("Invalid surname!");
            validateErrors.add("Некорректная фамилия!");
        }

        if (!scoringDataDTO.getMiddleName().isBlank() && !middleName.matches(namesCheckRegex)) {
            log.error("Invalid middle name!");
            validateErrors.add("Некорректное отчество!");
        }

        if (scoringDataDTO.getAmount().compareTo(minimalAmount) < 0) {
            log.error("Invalid amount! Must be greater or equal to 10000");
            validateErrors.add("Некорректная сумма!");
        }

        if (scoringDataDTO.getTerm() < minimalTerm) {
            log.error("Invalid term!");
            validateErrors.add("Некорректный срок кредита!");
        }

        if (!passportSeries.matches(passportSeriesCheckRegex)) {
            log.error("Invalid passport series!");
            validateErrors.add("Некорректная серия паспорта!");
        }

        if (!passportNumber.matches(passportNumberCheckRegex)) {
            log.error("Invalid passport number!");
            validateErrors.add("Некорректный номер паспорта!");
        }

        if (!birthdateMatcher.find()) {
            log.error("Invalid birth date!");
            validateErrors.add("Некорректная дата рождения!");
        }

        long fullYears = ChronoUnit.YEARS.between(scoringDataDTO.getBirthdate(), LocalDate.now());

        if (fullYears < minimalValidateAge || fullYears > maximumValidateAge) {
            log.error("Age must be between 20 and 60!");
            validateErrors.add("Кредит не может быть выдан по причине неподходящего возраста.");
        }

        if (employmentDTO.getEmploymentStatus() == EmploymentDTO.EmploymentStatus.UNEMPLOYED) {
            log.error("The loan was refused due to unemployment!");
            validateErrors.add("Кредит не может быть выдан по причине безработицы.");
        }

        BigDecimal maxAmount = employmentDTO.getSalary().multiply(BigDecimal.valueOf(20));  //максимальная сумма кредита

        if (scoringDataDTO.getAmount().compareTo(maxAmount) > 0) {
            log.error("The loan amount exceeds the maximum amount");
            validateErrors.add("Кредит не может быть выдан. Сумма кредита превышает макссимальное значение.");
        }

        if (employmentDTO.getWorkExperienceTotal() < workExperienceTotal) {
            log.error("Too little work experience");
            validateErrors.add("Кредит не может быть выдан по причине маленького опыта работы.");
        }

        if (employmentDTO.getWorkExperienceCurrent() < workExperienceCurrent) {
            log.error("Too little work experience");
            validateErrors.add("Кредит не может быть выдан по причине маленького опыта работы.");
        }

        if (!validateErrors.isEmpty()) {
            String joinedValidateErrors = validateErrors.stream()
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, joinedValidateErrors, null);
        }
    }

    public CreditDTO calculation(ScoringDataDTO scoringDataDTO,
                                 boolean isInsuranceEnabled,
                                 boolean isSalaryClient) {

        EmploymentDTO employmentDTO = scoringDataDTO.getEmployment();

        //ВАЛИДАЦИЯ ДАННЫХ

        BigDecimal rate = baseRate;

        long fullYears = ChronoUnit.YEARS.between(scoringDataDTO.getBirthdate(), LocalDate.now());

        if (employmentDTO.getEmploymentStatus() == EmploymentDTO.EmploymentStatus.SELFEMPLOYED) {
            log.info("The rate increases by 0.5 %");
            rate = rate.add(BigDecimal.valueOf(0.5));
        }

        if (employmentDTO.getEmploymentStatus() == EmploymentDTO.EmploymentStatus.BUSINESSOWNER) {
            log.info("The rate increases by 1.5 %");
            rate = rate.add(BigDecimal.valueOf(1.5));
        }

        if (employmentDTO.getPosition() == EmploymentDTO.Position.MIDDLEMANAGER) {
            log.info("The rate decreases by 1.0 %");
            rate = rate.subtract(BigDecimal.valueOf(1));
        }

        if (employmentDTO.getPosition() == EmploymentDTO.Position.TOPMANAGER) {
            log.info("The rate decreases by 2.0 %");
            rate = rate.subtract(BigDecimal.valueOf(2));
        }

        if (scoringDataDTO.getMaritalStatus() == ScoringDataDTO.MaritalStatus.MARRIED) {
            log.info("The rate decreases by 1.5 %");
            rate = rate.subtract(BigDecimal.valueOf(1.5));
        }

        if (scoringDataDTO.getMaritalStatus() == ScoringDataDTO.MaritalStatus.DIVORCED) {
            log.info("The rate increases by 0.5 %");
            rate = rate.add(BigDecimal.valueOf(0.5));
        }

        if (scoringDataDTO.getDependentAmount() > 1) {
            log.info("The rate increases by 0.5 %");
            rate = rate.add(BigDecimal.valueOf(0.5));
        }

        if (scoringDataDTO.getGender() == ScoringDataDTO.Gender.FEMALE && fullYears >= minimalCalculationAge) {
            log.info("The rate decreases by 1.5 %");
            rate = rate.subtract(BigDecimal.valueOf(1.5));
        }

        if (scoringDataDTO.getGender() == ScoringDataDTO.Gender.MALE
                && (fullYears >= minimalCalculationAge || fullYears < maximumCalculationAge)) {
            log.info("The rate decreases by 1.5 %");
            rate = rate.subtract(BigDecimal.valueOf(1.5));
        }

        if (scoringDataDTO.getGender() == ScoringDataDTO.Gender.NONBINARY) {
            log.info("The rate increases by 1.5 %");
            rate = rate.add(BigDecimal.valueOf(1.5));
        }

        // РАСЧЕТ ПАРАМЕТРОВ КРЕДИТА

        BigDecimal insurance = scoringDataDTO.getAmount().multiply(BigDecimal.valueOf(0.01));

        if (isInsuranceEnabled) {
            if (isSalaryClient) {

                log.info("Calculation loan offer for salary client with insurance");

                BigDecimal rateWithInsuranceAndSalary = rate.subtract(BigDecimal.valueOf(2)); //ставка со страховкой и ЗП

                BigDecimal monthRate4 = rateWithInsuranceAndSalary.divide(BigDecimal.valueOf(1200), 5, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах со страховкой и ЗП

                BigDecimal annuityRatio4 = (monthRate4.multiply((monthRate4.add(BigDecimal.valueOf(1)))
                        .pow(scoringDataDTO.getTerm())))
                        .divide(((monthRate4.add(BigDecimal.valueOf(1)))
                                .pow(scoringDataDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING);//коэффициент аннуитета со страховкой и ЗП

                BigDecimal monthlyPayment4 = (scoringDataDTO.getAmount().add(insurance))
                        .multiply(annuityRatio4).setScale(2, RoundingMode.HALF_UP);

                return new CreditDTO(
                        scoringDataDTO.getAmount().add(insurance),
                        scoringDataDTO.getTerm(),
                        monthlyPayment4,
                        rateWithInsuranceAndSalary,
                        pscCalculate(scoringDataDTO.getAmount().add(insurance), scoringDataDTO.getTerm(), monthlyPayment4),
                        true,
                        true,
                        paymentScheduleElement(rateWithInsuranceAndSalary, scoringDataDTO.getAmount().add(insurance), scoringDataDTO.getTerm(), monthlyPayment4)
                );
            } else {

                log.info("Calculation loan offer with insurance");

                BigDecimal rateWithInsurance = rate.subtract(BigDecimal.valueOf(1.5)); //ставка со страховкой

                BigDecimal monthRate3 = rateWithInsurance.divide(BigDecimal.valueOf(1200), 5, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах со страховкой

                BigDecimal annuityRatio3 = (monthRate3.multiply((monthRate3.add(BigDecimal.valueOf(1)))
                        .pow(scoringDataDTO.getTerm())))
                        .divide(((monthRate3.add(BigDecimal.valueOf(1)))
                                .pow(scoringDataDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING); //коэффициент аннуитета со страховкой или ЗП

                BigDecimal monthlyPayment3 = (scoringDataDTO.getAmount().add(insurance))
                        .multiply(annuityRatio3).setScale(2, RoundingMode.HALF_UP);

                return new CreditDTO(
                        scoringDataDTO.getAmount().add(insurance),
                        scoringDataDTO.getTerm(),
                        monthlyPayment3,
                        rateWithInsurance,
                        pscCalculate(scoringDataDTO.getAmount().add(insurance), scoringDataDTO.getTerm(), monthlyPayment3),
                        true,
                        false,
                        paymentScheduleElement(rateWithInsurance, scoringDataDTO.getAmount().add(insurance), scoringDataDTO.getTerm(), monthlyPayment3)
                );
            }
        } else if (isSalaryClient) {

            log.info("Calculation loan offer for salary client");

            BigDecimal rateWithSalary = rate.subtract(BigDecimal.valueOf(0.5)); //ставка с ЗП

            BigDecimal monthRate2 = rateWithSalary.divide(BigDecimal.valueOf(1200), 5, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах с ЗП

            BigDecimal annuityRatio2 = (monthRate2.multiply((monthRate2.add(BigDecimal.valueOf(1)))
                    .pow(scoringDataDTO.getTerm())))
                    .divide(((monthRate2.add(BigDecimal.valueOf(1)))
                            .pow(scoringDataDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING); //коэффициент аннуитета со страховкой или ЗП

            BigDecimal monthlyPayment2 = scoringDataDTO.getAmount()
                    .multiply(annuityRatio2).setScale(2, RoundingMode.HALF_UP);

            return new CreditDTO(
                    scoringDataDTO.getAmount(),
                    scoringDataDTO.getTerm(),
                    monthlyPayment2,
                    rateWithSalary,
                    pscCalculate(scoringDataDTO.getAmount(), scoringDataDTO.getTerm(), monthlyPayment2),
                    false,
                    true,
                    paymentScheduleElement(rateWithSalary, scoringDataDTO.getAmount(), scoringDataDTO.getTerm(), monthlyPayment2)
            );
        } else {

            log.info("Calculation loan offer without insurance");

            BigDecimal monthRate1 = rate.divide(BigDecimal.valueOf(1200), 5, RoundingMode.CEILING); //месячная ставка по кредиту не в процентах

            BigDecimal annuityRatio1 = (monthRate1.multiply((monthRate1.add(BigDecimal.valueOf(1)))
                    .pow(scoringDataDTO.getTerm())))
                    .divide(((monthRate1.add(BigDecimal.valueOf(1)))
                            .pow(scoringDataDTO.getTerm())).subtract(BigDecimal.valueOf(1)), 5, RoundingMode.CEILING); //коэффициент аннуитета без страховки и зарплаты

            BigDecimal monthlyPayment1 = scoringDataDTO.getAmount()
                    .multiply(annuityRatio1).setScale(2, RoundingMode.HALF_UP);

            return new CreditDTO(
                    scoringDataDTO.getAmount(),
                    scoringDataDTO.getTerm(),
                    monthlyPayment1,
                    rate,
                    pscCalculate(scoringDataDTO.getAmount(), scoringDataDTO.getTerm(), monthlyPayment1),
                    false,
                    false,
                    paymentScheduleElement(rate, scoringDataDTO.getAmount(), scoringDataDTO.getTerm(), monthlyPayment1)
            );
        }
    }

    private BigDecimal pscCalculate(BigDecimal amount, Integer term, BigDecimal monthlyPayment) {

        log.info("Calculation full cost of the loan");

        LocalDate dateOfIssue = LocalDate.now();
        LocalDate endDate = dateOfIssue.plusMonths(term);
        List<LocalDate> totalDates = new ArrayList<>();
        while (!dateOfIssue.isAfter(endDate)) {
            totalDates.add(dateOfIssue);
            dateOfIssue = dateOfIssue.plusMonths(1);
        }

        BigDecimal[] payments = new BigDecimal[term + 1];
        payments[0] = amount.negate();
        for (int i = 1; i < payments.length; i++) {
            payments[i] = monthlyPayment;
        }

        double basePeriod = 30.0; //базовый период

        long numberOfBasePeriods = Math.round(365 / basePeriod); //число базовых периодов

        //заполним массив с количеством дней с даты выдачи до даты к-го платежа
        int[] days = new int[term + 1];
        for (int k = 0; k < term + 1; k++) {
            days[k] = (int) ChronoUnit.DAYS.between(totalDates.get(0), totalDates.get(k));
        }

        //посчитаем Ек и Qк для каждого платежа
        BigDecimal[] e = new BigDecimal[term + 1];
        BigDecimal[] q = new BigDecimal[term + 1];
        for (int k = 0; k < term + 1; k++) {
            e[k] = (BigDecimal.valueOf(days[k]).remainder(BigDecimal.valueOf(basePeriod)))
                    .divide(BigDecimal.valueOf(basePeriod), 5, RoundingMode.CEILING);
            q[k] = BigDecimal.valueOf(days[k]).divide(BigDecimal.valueOf(basePeriod), RoundingMode.FLOOR);
        }

        BigDecimal i = BigDecimal.valueOf(0);
        BigDecimal x = BigDecimal.valueOf(1.0);
        BigDecimal x1 = BigDecimal.valueOf(0);
        BigDecimal step = BigDecimal.valueOf(0.0000001);  //шаг приближения
        while (x.compareTo(BigDecimal.valueOf(0)) > 0) {
            x1 = x;
            x = BigDecimal.valueOf(0);
            for (int k = 0; k < term + 1; k++) {
                x = x.add(payments[k].divide(((BigDecimal.valueOf(1).add(e[k]
                        .multiply(i))).multiply((BigDecimal.valueOf(1).add(i))
                        .pow(q[k].intValue()))), 3, RoundingMode.CEILING));
            }
            i = i.add(step);
        }
        System.out.println(i);

        return i.multiply(BigDecimal.valueOf(numberOfBasePeriods)).multiply(BigDecimal.valueOf(100)).setScale(3, RoundingMode.CEILING);
    }

    private List<PaymentScheduleElement> paymentScheduleElement(BigDecimal rate,
                                                                BigDecimal amount,
                                                                int term,
                                                                BigDecimal monthlyPayment) {

        log.info("Calculation payment schedule");

        List<PaymentScheduleElement> paymentSchedule = new ArrayList<>();

        int number = 0;

        BigDecimal monthRate = rate.divide(BigDecimal.valueOf(100 * 12), 5, RoundingMode.CEILING);//месячная ставка по кредиту не в процентах

        System.out.println(monthRate);

        BigDecimal remainingDebt = amount;

        for (int i = 1; i < term + 1; i++) {

            BigDecimal interestPayment = remainingDebt.multiply(monthRate).setScale(2, RoundingMode.DOWN);

            BigDecimal debtPayment = monthlyPayment.subtract(interestPayment).setScale(2, RoundingMode.DOWN);

            remainingDebt = remainingDebt.subtract(debtPayment).setScale(2, RoundingMode.DOWN);

            paymentSchedule.add(new PaymentScheduleElement(
                    number + i,
                    LocalDate.now().plusMonths(i),
                    monthlyPayment,
                    interestPayment,
                    debtPayment,
                    remainingDebt));

        }

        if (remainingDebt.compareTo(BigDecimal.valueOf(0)) > 0) {

            paymentSchedule.add(new PaymentScheduleElement(
                    number + term + 1,
                    LocalDate.now().plusMonths(term),
                    remainingDebt,
                    BigDecimal.valueOf(0),
                    remainingDebt,
                    BigDecimal.valueOf(0)
            ));
        }

        return paymentSchedule;

    }
}