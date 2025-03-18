package jp.co.sss.shop.controller.client.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.shop.bean.BasketBean;
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
	public String orderCheck(@RequestParam("payMethod") Integer payMethod) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}

		// 支払方法の設定
		orderForm.setPayMethod(payMethod);
		session.setAttribute("orderForm", orderForm);
		return "redirect:/client/order/check";
	}

	/**
	 * 注文確認画面表示処理
	 */
	@GetMapping("/check")
	public String orderCheck(Model model) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		if (orderForm == null) {
			return "redirect:/client/order/address/input";
		}

		@SuppressWarnings("unchecked")
		List<BasketBean> orderItems = (List<BasketBean>) session.getAttribute("basketBeans");

		if (orderItems == null || orderItems.isEmpty()) {
			model.addAttribute("errorMessage", "買い物かごが空です。");
			return "redirect:/client/basket/list";
		}

		// 合計金額の計算
		int totalPrice = priceCalc.orderItemBeanPriceTotal(basketBeans);

		model.addAttribute("totalPrice", totalPrice);
		model.addAttribute("orderItems", orderItems);
		model.addAttribute("orderForm", orderForm);

		return "client/order/check";
	}

	/**
	 * 注文確定ボタン処理
	 */
	@RequestMapping(path = "/complete", method = { RequestMethod.POST })
	public String orderComplete() {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");

		@SuppressWarnings("unchecked")
		List<OrderItem> orderItems = (List<OrderItem>) session.getAttribute("basketBeanList");

		Order order = new Order();
		order.setPostalCode(orderForm.getPostalCode());
		order.setAddress(orderForm.getAddress());
		order.setName(orderForm.getName());
		order.setPhoneNumber(orderForm.getPhoneNumber());
		order.setPayMethod(orderForm.getPayMethod());
		order.setOrderItemsList(orderItems);

		orderRepository.save(order);
		session.removeAttribute("orderForm");
		session.removeAttribute("basketBeanList");
		return "redirect:/client/order/complete";
	}

	/**
	 * 注文完了画面表示処理
	 */
	@RequestMapping(path = "/complete", method = { RequestMethod.GET })
	public String orderCompleteFinish() {
		return "client/order/complete";
	}
}
