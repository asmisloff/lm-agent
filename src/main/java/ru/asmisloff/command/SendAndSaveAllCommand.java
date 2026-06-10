package ru.asmisloff.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Команда отправляет промпт и сохраняет в файлах весь код из ответа модели
 */
@Log4j2
@RequiredArgsConstructor
public class SendAndSaveAllCommand implements Command {

    private final SendPromptCommand sendCmd;
    private final SaveCodeCommand saveCmd;

    /**
     * Выполняет команду.
     *
     * @param args аргументы команды. Игнорируются.
     */
    @Override
    public void exec(String... args) {
        log.info("Запуск отправки промпта и сохранения всего кода из ответа");
        sendCmd.exec();
        saveCmd.exec(sendCmd.getProps().getAnswerFileName());
        log.info("Код из ответа сохранён");
    }
}