package jp.co.sss.shop.controller.client.order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.shop.bean.OrderBean;
import jp.co.sss.shop.bean.OrderItemBean;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.Order;
import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;

/**
 * 注文管理 一覧表示機能(一般会員用)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
@RequestMapping("/client/order")
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
	 * @param pageable ページング情報
	 * @return "client/order/list" 注文一覧画面
	 */
	@GetMapping("/list")
	public String orderList(Model model, Pageable pageable) {
		// セッションからログインユーザー情報を取得
		UserBean userBean = (UserBean) session.getAttribute("user");
		if (userBean == null) {
			return "redirect:/login"; // ログインしていない場合はログイン画面へ
		}

		// ユーザーの注文情報を取得
		Page<Order> ordersPage = orderRepository.findByUserIdOrderByDateDesc(userBean.getId(), pageable);

		// 注文情報リストを生成
		List<OrderBean> orderBeanList = new ArrayList<>();
		for (Order order : ordersPage.getContent()) {
			OrderBean orderBean = beanTools.copyEntityToOrderBean(order);

			// 合計金額を計算
			int total = priceCalc.orderItemPriceTotal(order.getOrderItemsList());
			orderBean.setTotal(total);

			orderBeanList.add(orderBean);
		}

		// **ページング情報を渡す**
		model.addAttribute("pages", ordersPage);
		model.addAttribute("orders", orderBeanList);

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
	@GetMapping("/detail/{id}")
	public String orderDetail(@PathVariable("id") Integer id, Model model) {
		// セッションからログインユーザー情報を取得
		UserBean userBean = (UserBean) session.getAttribute("user");
		if (userBean == null) {
			return "redirect:/login"; // ログインしていない場合はログイン画面へ
		}

		// 注文情報を取得
		Order order = orderRepository.findById(id).orElse(null);

		// 他のユーザーの注文なら一覧に戻す
		if (order == null || !order.getUser().getId().equals(userBean.getId())) {
			return "redirect:/client/order/list";
		}

		// BeanToolsを使用して注文情報をBeanに変換
		OrderBean orderBean = beanTools.copyEntityToOrderBean(order);
		// 日付を適切な形式でフォーマット
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String formattedDate = sdf.format(order.getInsertDate()); // Date → String に変換
		orderBean.setInsertDate(formattedDate); // フォーマットした日付をセット
		// 注文商品のリストを取得
		List<OrderItemBean> orderItemBeanList = beanTools.generateOrderItemBeanList(order.getOrderItemsList());
		// **注文の合計金額を計算**
		int total = priceCalc.orderItemPriceTotal(order.getOrderItemsList());

		// 注文情報をViewへ渡す
		model.addAttribute("order", orderBean);
		model.addAttribute("orderItemBeans", orderItemBeanList);
		model.addAttribute("total", total);
		model.addAttribute("formattedInsertDate", formattedDate);
		return "client/order/detail";
	}

}
