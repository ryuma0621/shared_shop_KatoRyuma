package jp.co.sss.shop.controller.client.order;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;

import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;

/**
 * 注文管理 一覧表示機能(一般会員用)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientOrderShowController {

	/**
	 * 注文情報
	 */
	@Autowired
	OrderRepository orderRepository;

	/**
	 * セッション
	 */
	@Autowired
	HttpSession session;
	/**
	 * 合計金額計算サービス
	 */
	@Autowired
	PriceCalc priceCalc;
	/**
	 * Entity、Form、Bean間のデータ生成、コピーサービス
	 */
	@Autowired
	BeanTools beanTools;

	/**
	 * 注文情報一覧表示処理。
	 * セッションからユーザ情報を取得。
	 * ユーザ情報を引数に注文情報を取得する。
	 * 注文情報から注文商品情報を取得し、１注文ごとの合計金額を計算する
	 * 注文情報と合計金額を注文情報Beanにセットする
	 * modelスコープに注文情報Beanを登録する
	 *
	 * @param model    Viewとの値受渡し
	 * @return "client/order/list" 注文一覧画面
	 */
	//TODO こちらに処理を記述してください
	//	@RequestMapping(path = "####", method = { RequestMethod.GET, RequestMethod.POST })
	public String showOrderList(Model model) {

		return "client/order/list";
	}

	/**
	 * 詳細表示処理
	 * URLから注文IDを取得する。
	 * 注文情報から注文情報Beanにデータを移し替える
	 * 注文情報から注文商品情報を取得し、１注文ごとの合計金額を計算する
	 * 注文情報と合計金額を注文情報Beanにセットする 
	 *
	 * @param id   詳細表示対象ID
	 * @param model   Viewとの値受渡し
	 * @return "client/order/detail" 注文情報詳細画面
	 */
	//TODO こちらに処理を記述してください
	//	@RequestMapping(path = "/client/order/detail/{id}")
	public String showOrder(@PathVariable int id, Model model) {

		return "client/order/detail";

	}

}
