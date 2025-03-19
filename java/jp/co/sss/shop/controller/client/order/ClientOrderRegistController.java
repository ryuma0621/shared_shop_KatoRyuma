package jp.co.sss.shop.controller.client.order;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.shop.bean.BasketBean;
import jp.co.sss.shop.bean.OrderBean;
import jp.co.sss.shop.bean.OrderItemBean;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.Order;
import jp.co.sss.shop.entity.OrderItem;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.form.OrderForm;
import jp.co.sss.shop.repository.ItemRepository;
import jp.co.sss.shop.repository.OrderItemRepository;
import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;

@Controller
@RequestMapping("/client/order")
public class ClientOrderRegistController {

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	OrderItemRepository orderItemRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	HttpSession session;

	@Autowired
	PriceCalc priceCalc;

	@Autowired
	BeanTools beanTools;

	/**
	 * ご注文のお手続きボタン 押下時処理
	 */
	@RequestMapping(path = "/address/input", method = { RequestMethod.POST })
	public String addressInputRedirect() {
		OrderForm orderForm = new OrderForm();

		// セッションスコープからログインユーザー情報を取得
		UserBean userBean = (UserBean) session.getAttribute("user");
		if (userBean == null) {
			return "redirect:/login";
		}

		// DBから最新のユーザー情報を取得
		User user = userRepository.getReferenceById(userBean.getId());

		// 取得したユーザー情報を注文フォームに設定
		orderForm.setId(user.getId());
		orderForm.setPostalCode(user.getPostalCode());
		orderForm.setAddress(user.getAddress());
		orderForm.setName(user.getName());
		orderForm.setPhoneNumber(user.getPhoneNumber());

		// 支払い方法の初期値をクレジットカードに設定
		orderForm.setPayMethod(1);

		// セッションに保存
		session.setAttribute("orderForm", orderForm);
		return "redirect:/client/order/address/input";
	}

	/**
	 * 届け先入力画面表示処理
	 */
	@RequestMapping(path = "/address/input", method = { RequestMethod.GET })
	public String addressInput(Model model) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}
		model.addAttribute("orderForm", orderForm);
		return "client/order/address_input";
	}

	/**
	 * 届け先入力 次へボタン処理
	 */
	@RequestMapping(path = "/payment/input", method = { RequestMethod.POST })
	public String inputAddressCheck(@Valid @ModelAttribute OrderForm form, BindingResult result) {
		if (result.hasErrors()) {
			session.setAttribute("orderForm", form);
			return "redirect:/client/order/address/input";
		}
		session.setAttribute("orderForm", form);
		return "redirect:/client/order/payment/input";
	}

	/**
	 * 支払方法選択画面表示処理
	 */
	@RequestMapping(path = "/payment/input", method = { RequestMethod.GET })
	public String paymentInput(Model model) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}
		model.addAttribute("orderForm", orderForm);
		return "client/order/payment_input";
	}

	/**
	 * 支払方法選択 次へボタン処理
	 */
	@RequestMapping(path = "/check", method = { RequestMethod.POST })
	public String orderCheck(@RequestParam(value = "payMethod", required = false) Integer payMethod) {
		// セッションから orderForm を取得
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		// 注文情報がセッションにない場合、住所入力画面に戻る
		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}

		// 支払方法が未選択なら、クレジットカード(1)を設定
		if (payMethod == null) {
			payMethod = 1;
		}

		// 支払方法の設定
		orderForm.setPayMethod(payMethod);
		session.setAttribute("orderForm", orderForm);

		// カートが空でないか再確認
		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeans = (List<BasketBean>) session.getAttribute("basketBeans");

		if (basketBeans == null || basketBeans.isEmpty()) {
			return "redirect:/client/basket/list";
		}

		return "redirect:/client/order/check";
	}

	/**
	 * 支払方法選択画面で「戻る」ボタン押下時
	 * 届け先入力画面に戻る
	 */
	@RequestMapping(path = "/payment/back", method = { RequestMethod.POST })
	public String paymentBack() {
		return "redirect:/client/order/address/input";
	}

	/**
	 * 注文確認画面表示処理
	 */
	@GetMapping("/check")
	public String orderCheck(Model model) {
		// セッションから注文フォームを取得
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		// 注文情報がセッションにない場合、住所入力画面に戻る
		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}

		// 支払い方法が未設定なら、クレジットカード(1)を設定
		if (orderForm.getPayMethod() == null) {
			orderForm.setPayMethod(1);
			session.setAttribute("orderForm", orderForm);
		}

		// セッションから `basketBeans` を取得
		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeans = (List<BasketBean>) session.getAttribute("basketBeans");

		// 買い物かごが空の場合、カート画面にリダイレクト
		if (basketBeans == null || basketBeans.isEmpty()) {
			session.setAttribute("errorMessage", "買い物かごが空です。");
			return "redirect:/client/basket/list";
		}

		// BasketBean を OrderItemBean に変換
		List<OrderItemBean> orderItemBeans = basketBeans.stream().map(bean -> {
			OrderItemBean orderItemBean = new OrderItemBean();
			orderItemBean.setId(bean.getId());
			orderItemBean.setName(bean.getName());
			orderItemBean.setPrice(itemRepository.findById(bean.getId()).get().getPrice()); // 商品の価格
			orderItemBean.setOrderNum(bean.getOrderNum()); // 注文個数を正しくセット
			orderItemBean.setImage(itemRepository.findById(bean.getId()).get().getImage()); // 商品画像をセット

			// 小計 (subtotal) = 単価 × 注文数
			orderItemBean.setSubtotal(orderItemBean.getPrice() * orderItemBean.getOrderNum());

			return orderItemBean;
		}).collect(Collectors.toList());

		// 合計金額を計算
		int total = orderItemBeans.stream().mapToInt(OrderItemBean::getSubtotal).sum();

		// リクエストスコープに設定
		model.addAttribute("orderItemBeans", orderItemBeans);
		model.addAttribute("total", total);
		model.addAttribute("orderForm", orderForm);

		return "client/order/check";
	}

	/**
	 * 注文確認画面で「戻る」ボタン押下時
	 * 支払い方法選択画面に戻る
	 */
	@RequestMapping(path = "/check/back", method = { RequestMethod.POST })
	public String checkBack() {
		return "redirect:/client/order/payment/input";
	}

	/**
	 * 注文確定ボタン処理
	 */
	@RequestMapping(path = "/complete", method = { RequestMethod.POST })
	public String orderComplete() {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}

		// セッションからログインユーザーを取得
		UserBean userBean = (UserBean) session.getAttribute("user");
		if (userBean == null) {
			return "redirect:/login"; // ログイン情報がない場合はログイン画面へ
		}

		// 買い物かごの情報を取得
		@SuppressWarnings("unchecked")
		List<BasketBean> basketBeans = (List<BasketBean>) session.getAttribute("basketBeans");

		if (basketBeans == null || basketBeans.isEmpty()) {
			return "redirect:/client/basket/list";
		}

		// 注文情報を作成
		Order order = new Order();
		order.setPostalCode(orderForm.getPostalCode());
		order.setAddress(orderForm.getAddress());
		order.setName(orderForm.getName());
		order.setPhoneNumber(orderForm.getPhoneNumber());
		order.setPayMethod(orderForm.getPayMethod());

		// ユーザーIDをセット
		User user = userRepository.getReferenceById(userBean.getId());
		order.setUser(user);

		// 注文商品リストを作成
		List<OrderItem> orderItems = basketBeans.stream().map(bean -> {
			OrderItem orderItem = new OrderItem();
			orderItem.setItem(itemRepository.findById(bean.getId()).orElse(null));
			orderItem.setQuantity(bean.getOrderNum());

			// 価格が NULL にならないようにする
			int price = orderItem.getItem() != null ? orderItem.getItem().getPrice() : 0;
			orderItem.setPrice(price);

			orderItem.setOrder(order);
			return orderItem;
		}).toList();

		order.setOrderItemsList(orderItems);

		// DBに保存
		orderRepository.save(order);

		// セッションの注文関連データを削除
		session.removeAttribute("orderForm");
		session.removeAttribute("basketBeans");

		return "redirect:/client/order/complete";
	}

	/**
	 * 注文完了画面表示処理
	 */
	@RequestMapping(path = "/complete", method = { RequestMethod.GET })
	public String orderCompleteFinish() {
		return "client/order/complete";
	}

	/**
	 * 注文一覧画面の表示処理
	 * 
	 * @param model    ビューへのデータ送信
	 * @param pageable ページング情報
	 * @return 注文一覧画面
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
			// BeanToolsを使用して注文情報をBeanに変換
			OrderBean orderBean = beanTools.copyEntityToOrderBean(order);
			// 注文の合計金額を計算
			int total = priceCalc.orderItemPriceTotal(order.getOrderItemsList());
			// 合計金額をセット
			orderBean.setTotal(total);
			orderBeanList.add(orderBean);
		}

		// 注文情報リストをViewへ渡す
		model.addAttribute("pages", ordersPage);
		model.addAttribute("orders", orderBeanList);

		return "client/order/list";
	}

	/**
	 * 注文詳細画面の表示処理
	 * 
	 * @param id    注文ID
	 * @param model ビューへのデータ送信
	 * @return 注文詳細画面
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
		// 注文商品のリストを取得
		List<OrderItemBean> orderItemBeanList = beanTools.generateOrderItemBeanList(order.getOrderItemsList());
		// 注文の合計金額を計算
		int total = priceCalc.orderItemBeanPriceTotalUseSubtotal(orderItemBeanList);

		// 注文情報をViewへ渡す
		model.addAttribute("order", orderBean);
		model.addAttribute("orderItemBeans", orderItemBeanList);
		model.addAttribute("total", total);

		return "client/order/detail";
	}
}
