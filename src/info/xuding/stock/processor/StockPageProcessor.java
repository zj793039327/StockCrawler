package info.xuding.stock.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.http.client.HttpClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import info.xuding.stock.dao.StockDao;
import info.xuding.stock.model.TopBill;
import info.xuding.stock.utils.StockPriceUtils;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

/**
 * @author xuding
 *
 */
@Component
public class StockPageProcessor implements PageProcessor {

	private static final String DATA_URL = "(http://data.10jqka.com.cn/market/lhbcjmx/code/\\w+/)";

	@Resource
	private StockDao stockDao;

	@Override
	public void process(Page page) {
		String thisUrl = page.getUrl().toString();
		String date = thisUrl.substring(thisUrl.indexOf("date/") + 5, thisUrl.indexOf("date/") + 15);
		List<String> urlList = page.getHtml().$("#maintable > tbody > tr > td").links().regex(DATA_URL).all();
		for (String str : urlList) {
			str += "date/" + date + "/ajax/";
			page.addTargetRequest(str);
		}
		String stockName = page.getHtml().$("div.stock_name h2 strong").xpath("tidyText()").toString();
		if (stockName == null) {
			return;
		}
		String stockCode = page.getHtml().$("#pageStockCode").xpath("tidyText()").toString();
		String stockDate = page.getHtml().$("select.select option[selected]").xpath("tidyText()").toString();
		Selectable table = page.getHtml().$(".m_table").nodes().get(0);
		List<Selectable> list = table.$("tbody > tr").nodes();
		for (int i = 0; i < list.size(); i++) {
			try {
				Selectable selectable = list.get(i);
				TopBill topBill = new TopBill(stockDate, stockName, stockCode, 0, 0, 0);
				topBill.setOrganization(selectable.xpath("/tr/td[2]/a/text()").toString());
				if (topBill.getOrganization() == null) {
					continue;
				}
				topBill.setBuyAmount(Double.valueOf(selectable.xpath("tr/td[3]/text()").toString()));
				topBill.setBuyPercent(selectable.xpath("tr/td[4]/text()").toString());
				topBill.setSellAmount(Double.valueOf(selectable.xpath("tr/td[5]/text()").toString()));
				topBill.setSellPercent(selectable.xpath("tr/td[6]/text()").toString());
				topBill.setNetAmount(Double.valueOf(selectable.xpath("tr/td[7]/text()").toString()));
				topBill.setTurnover(Double.valueOf(selectable.xpath("tr/td[8]/text()").toString()));
				topBill.setPrice(StockPriceUtils.getStockPrice(stockCode));
				stockDao.add(topBill);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public Site getSite() {
		return Site.me().setRetryTimes(3).setSleepTime(0);
	}

	// public static void main(String[] args) {
	// ApplicationContext applicationContext = new
	// ClassPathXmlApplicationContext(
	// "classpath:/spring/applicationContext*.xml");
	// StockPageProcessor stockPageProcessor =
	// applicationContext.getBean(StockPageProcessor.class);
	// Spider.create(stockPageProcessor).addUrl("http://data.10jqka.com.cn/market/lhbcjmx/code/000780/date/2016-04-26/ajax/").thread(5)
	// .run();
	// }

}
