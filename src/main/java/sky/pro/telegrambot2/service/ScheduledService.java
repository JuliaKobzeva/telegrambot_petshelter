package sky.pro.telegrambot2.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import sky.pro.telegrambot2.enam.ProbationaryStatus;
import sky.pro.telegrambot2.model.CatOwner;
import sky.pro.telegrambot2.model.CatOwnerReport;
import sky.pro.telegrambot2.model.DogOwner;
import sky.pro.telegrambot2.model.DogOwnerReport;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduledService {
    private final DogOwnerService dogOwnerService;
    private final CatOwnerService catOwnerService;
    private final DogOwnerReportService dogOwnerReportService;
    private final CatOwnerReportService catOwnerReportService;
    private final TelegramBot telegramBot;

    public ScheduledService(DogOwnerService dogOwnerService,
                            CatOwnerService catOwnerService,
                            DogOwnerReportService dogOwnerReportService,
                            CatOwnerReportService catOwnerReportService,
                            TelegramBot telegramBot) {
        this.dogOwnerService = dogOwnerService;
        this.catOwnerService = catOwnerService;
        this.dogOwnerReportService = dogOwnerReportService;
        this.catOwnerReportService = catOwnerReportService;
        this.telegramBot = telegramBot;
    }

    @Scheduled(fixedDelay = 59_000L)
    public void informOwner() {
        List<DogOwner> dogOwners = dogOwnerService.findAllOwners();
        List<DogOwnerReport> dogOwnerReports = dogOwnerReportService.findAllReports();
        List<CatOwner> catOwners = catOwnerService.findAllOwners();
        List<CatOwnerReport> catOwnerReports = catOwnerReportService.findAllReports();
        informOwnerWhenHePassed(dogOwners, catOwners);
        informOwnerWhenHeNotPassed(dogOwners, catOwners);
        informOwnerWhenHeBadReporting(dogOwners, catOwners);
        informOwnerWhenDeadlineExtended(dogOwners, catOwners);
        checkDeadline(dogOwnerReports,catOwnerReports);
    }


    /* Если овнер прошел исп срок, волонтер меняет статус на PASSED, метод проверяет всех овнеров на
     * данный статус если овнеры найдены, бот информирует их о прохождении исп срока, далее метод меняет статус
     * у всех овнеровна FINALLY_PASSED чтобы метод больше не информировал овнеров о прохождении исп срока.*/
    private void informOwnerWhenHePassed(List<DogOwner> dogOwners,
                                         List<CatOwner> catOwners) {
        dogOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.PASSED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Добрый день, поздравляем" +
                                " ваш испытательный срок окончен")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.PASSED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_PASSED))
                .forEach(dogOwnerService::saveOwner);

        catOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.PASSED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Добрый день, поздравляем" +
                                " ваш испытательный срок окончен")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.PASSED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_PASSED))
                .forEach(catOwnerService::saveOwner);
    }


    /*  Если овнер прошел исп срок, волонтер меняет статус на NOT_PASSED, метод проверяет всех овнеров на данный статус
     * если овнеры найдены, бот информирует их о прохождении исп срока, далее метод меняет статус у всех овнеров
     * на FINALLY_PASSED чтобы метод больше не информировал овнеров о прохождении исп срока.*/

    private void informOwnerWhenHeNotPassed(List<DogOwner> dogOwners,
                                            List<CatOwner> catOwners) {
        dogOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.NOT_PASSED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Добрый день" +
                                " к сожалению вы не прошли испытательный срок, пожалуйста верните" +
                                " животное в приют.")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.NOT_PASSED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_NOT_PASSED))
                .forEach(dogOwnerService::saveOwner);

        catOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.NOT_PASSED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Добрый день" +
                                " к сожалению вы не прошли испытательный срок, пожалуйста верните" +
                                " животное в приют.")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.NOT_PASSED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_NOT_PASSED))
                .forEach(catOwnerService::saveOwner);
    }


    /* Если просмотрев отчеты овнеров волонтер решил, что овнер предоставляет отчеты плохо, волонтер меняет
     * статус овнера на BAD_REPORTING, далее метод находит овнеров с таким статусом бот информирует овнера, что он
     * предоставляет отчеты плохо и просит исправиться, далее метод меняет статус у всех овнеров
     * на UNSATISFACTORY чтобы метод больше не информировал овнеров о прохождении исп срока
     * */
    private void informOwnerWhenHeBadReporting(List<DogOwner> dogOwners,
                                               List<CatOwner> catOwners) {
        dogOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.BAD_REPORTING))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Дорогой усыновитель, мы заметили, что вы заполняете" +
                                " отчет не так подробно, как необходимо. Пожалуйста, подойди ответственнее к этому занятию." +
                                " В противном случае волонтеры приюта будут обязаны самолично проверять условия" +
                                " содержания собаки")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.BAD_REPORTING))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.UNSATISFACTORY))
                .forEach(dogOwnerService::saveOwner);

        catOwners.stream().filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.BAD_REPORTING))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Дорогой усыновитель, мы заметили, что вы заполняете" +
                                " отчет не так подробно, как необходимо. Пожалуйста, подойди ответственнее к этому занятию." +
                                " В противном случае волонтеры приюта будут обязаны самолично проверять условия" +
                                " содержания собаки")))
                .filter(element -> element.getProbationaryStatus().equals(ProbationaryStatus.BAD_REPORTING))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.UNSATISFACTORY))
                .forEach(catOwnerService::saveOwner);
    }


    /* Если волантер решил продлить исп срок овнера, он должен вручную добавить срок продления в бд и
     * поменять статус усыновителя на EXTENDED. Далее метод проверяет у овнеров срок продления и статус,
     * если срок продления больше 0 и статус равен EXTENDED бот информирует овнера о продлении исп срока. Далее метод
     * перезаписывает статус овнера на FINALLY_EXTENDED, чтобы бот повторно не информировал овнера
     * */
    private void informOwnerWhenDeadlineExtended(List<DogOwner> dogOwners,
                                                 List<CatOwner> catOwners) {
        dogOwners.stream().filter(element -> element.getPeriodExtend() > 0
                        && element.getProbationaryStatus().equals(ProbationaryStatus.EXTENDED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Дорогой усыновитель, ваш испытаельный срок продлен на " +
                                element.getPeriodExtend() + " дней")))
                .filter(element -> element.getPeriodExtend() > 0
                        && element.getProbationaryStatus().equals(ProbationaryStatus.EXTENDED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_EXTENDED))
                .forEach(dogOwnerService::saveOwner);

        catOwners.stream().filter(element -> element.getPeriodExtend() > 0
                        && element.getProbationaryStatus().equals(ProbationaryStatus.EXTENDED))
                .peek(element -> telegramBot.execute(
                        new SendMessage(element.getChatId(), "Дорогой усыновитель, ваш испытаельный срок продлен на " +
                                element.getPeriodExtend() + " дней")))
                .filter(element -> element.getPeriodExtend() > 0
                        && element.getProbationaryStatus().equals(ProbationaryStatus.EXTENDED))
                .peek(element -> element.setProbationaryStatus(ProbationaryStatus.FINALLY_EXTENDED))
                .forEach(catOwnerService::saveOwner);
    }


    /* дату последнего отчета я увеличиваю на один день, если увеличенная дата будет равна настоящей дате
     * бот проинформирует пользователя, что он плохо предоставляет отчеты. Далее дату последнего отчета я увеличиваю
     * на два дня, если увеличенная дата будет равна настоящей дате бот свяжется с волантером и предоставит ему
     * данные на пользователя который плохо заполняет отчеты*/
    private void checkDeadline(List<DogOwnerReport> dogOwnerReports,
                               List<CatOwnerReport> catOwnerReports) {

        LocalDateTime localDateTimeNow = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        dogOwnerReports.forEach(element -> {
            if (element.getDateOfLastReport().plusMinutes(1).equals(localDateTimeNow)) {
                informOwner(element.getDogOwner().getChatId());
            } else if (element.getDateOfLastReport().plusMinutes(2).equals(localDateTimeNow)) {
                informVolunteer(element.getDogOwner().getChatId(),element.getDogOwner().getName());
            }
        });

        catOwnerReports.forEach(element -> {
            if (element.getDateOfLastReport().plusMinutes(1).equals(localDateTimeNow)) {
                informOwner(element.getCatOwner().getChatId());
            } else if (element.getDateOfLastReport().plusMinutes(2).equals(localDateTimeNow)) {
                informVolunteer(element.getCatOwner().getChatId(),element.getCatOwner().getName());
            }
        });
    }

    /* если с даты последнего отчета прошел один день, этот метод информирует овнера, чтобы он
     * заполнял отчеты лучше
     */
    private void informOwner(Long chatId) {
        telegramBot.execute(new SendMessage(chatId, "Дорогой усыновитель, мы заметили," +
                " что за последние сутки вы предоставляли не подробные отчеты о животном, пожалуйста" +
                " отнеситесь серьезно к предоставлению отчетов"));
    }

    /* если с даты последнего отчета прошло два дня, этот метод информирует волонтера,
     * что овнер плохо заполняет отчеты
     */
    private void informVolunteer(Long chatId, String name) {
        Long VOLUNTEER_CHAT_ID = 5102380657L;
        telegramBot.execute(new SendMessage(VOLUNTEER_CHAT_ID, "Пользователь," +
                " по имени: " + name + " id: " + chatId + " более двух суток не" +
                " заполнял отчет, пожалуйста свяжитесь с ним"));
    }
}
