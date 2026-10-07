package dio.budgeting.application;

import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class GetTotalSpendByCategoryUseCase {
    private final TransactionRepository transactionRepository;

    public GetTotalSpendByCategoryUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Tool(name = "get-total-spend-by-category", description = "Calcula e retorna a soma total dos gastos para uma determinada categoria")
    public double execute(@ToolParam(description = "Categoria de uma transação") Category category) {
        long total = transactionRepository.sumAmountByCategory(category);
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
