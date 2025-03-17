package jp.co.sss.shop.controller.client.order;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.entity.Order;
import jp.co.sss.shop.entity.OrderItem;
import jp.co.sss.shop.form.OrderForm;
import jp.co.sss.shop.repository.ItemRepository;
import jp.co.sss.shop.repository.OrderItemRepository;
import jp.co.sss.shop.repository.OrderRepository;
import jp.co.sss.shop.service.BeanTools;
import jp.co.sss.shop.service.PriceCalc;

/**
 * 注文管理 注文受付機能のコントローラクラス
 */
@Controller
public class ClientOrderRegistController {

	@Autowired
	ItemRepository itemRepository;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	OrderItemRepository orderItemRepository;

	@Autowired
	HttpSession session;

	@Autowired
	PriceCalc priceCalc;

	@Autowired
	BeanTools beanTools;

	/**
	 * お届け先情報入力画面　表示処理
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/order/address_input" お届け先入力画面
	 */
	@GetMapping("/client/order/address/input")
	public String inputAddress(Model model) {
		model.addAttribute("orderForm", new OrderForm()); // 新しいOrderFormを渡す
		return "redirect:/client/order/address_input"; // お届け先入力画面
	}

	/**
	 * 届け先入力フォーム　チェック処理
	 *
	 * @param form 入力フォーム
	 * @param result 入力チェック結果
	 * @return 入力値エラーあり："redirect:/client/order/address/input" 送付先入力画面 表示処理
	 *         入力値エラーなし："redirect:/client/order/payment/input" 支払方法入力画面　表示処理 
	 */
	@PostMapping("/client/order/address/check")
	public String inputAddressCheck(@Valid @ModelAttribute OrderForm form, BindingResult result) {
		if (result.hasErrors()) {
			return "redirect:/client/order/address/input"; // 入力エラーがあった場合、再入力画面に戻る
		}
		session.setAttribute("orderForm", form); // セッションに注文フォームを保存
		return "redirect:/client/order/payment/input"; // 支払方法入力画面へ遷移
	}

	/**
	 * 注文内容確認処理(お支払い方法取得)
	 *
	 * @param payMethod 支払方法　入力
	 * @return "redirect:/client/order/check" 注文情報登録確認画面 表示
	 */
	@PostMapping("/client/order/check")
	public String orderCheck(@RequestParam Integer payMethod) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		orderForm.setPayMethod(payMethod);
		session.setAttribute("orderForm", orderForm);
		return "redirect:/client/order/check"; // 注文確認画面へ遷移
	}

	/**
	 * 注文内容在庫チェックと注文確認画面　表示処理
	 * 
	 * @param model Viewとの値受渡し
	 * @return "client/order/check" 注文確認画面　表示
	 *  sessionからorderFormまたはbasketBeanListが取得できない "redirect:/syserror" エラー画面
	 */
	@GetMapping("/client/order/check")
	public String orderCheck(Model model) {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror"; // セッションから取得できない場合、エラー画面へ
		}

		// セッションから注文商品リストを取得
		@SuppressWarnings("unchecked")
		List<OrderItem> orderItems = (List<OrderItem>) session.getAttribute("basketBeanList");
		for (OrderItem orderItem : orderItems) {
			Optional<Item> item = itemRepository.findById(orderItem.getItem().getId()); // Itemを取得
			if (item.isPresent()) {
				int stock = item.get().getStock();
				if (orderItem.getQuantity() > stock) {
					model.addAttribute("errorMessage", "在庫数が不足しています。");
					return "client/order/check"; // 在庫不足メッセージ表示
				}
			}
		}
		return "client/order/check"; // 注文確認画面へ遷移
	}

	/**
	 * 注文登録完了の処理。注文を行う際には商品テーブル、注文テーブル、注文商品テーブルへの3テーブルへ登録、更新を行う。
	 * 商品テーブル：商品の在庫数から購入数を引いて、DB更新
	 * 注文テーブル：1件分の注文レコードを登録する
	 * 注文商品テーブル：商品の種別ごとにレコードを追加する
	 * 最後にセッションから買い物かごリストと注文情報を削除する
	 *
	 * @return 在庫切れありの場合："redirect:/client/order/check" 注文確認画面　表示処理
	 *         在庫切れなしの場合："redirect:/client/order/complete" 注文完了画面　表示処理
	 */
	@PostMapping("/client/order/complete")
	public String orderComplete() {
		OrderForm orderForm = (OrderForm) session.getAttribute("orderForm");
		if (orderForm == null) {
			return "redirect:/syserror"; // セッションがない場合、エラー画面に遷移
		}

		// 注文エンティティに変換
		Order order = new Order(); // 新しいOrderエンティティを作成
		order.setPostalCode(orderForm.getPostalCode());
		order.setAddress(orderForm.getAddress());
		order.setName(orderForm.getName());
		order.setPhoneNumber(orderForm.getPhoneNumber());
		order.setPayMethod(orderForm.getPayMethod());

		// セッションから取得した注文商品リストを追加
		@SuppressWarnings("unchecked")
		List<OrderItem> orderItems = (List<OrderItem>) session.getAttribute("basketBeanList");
		for (OrderItem orderItem : orderItems) {
			orderItem.setOrder(order); // 注文商品に紐づけ
			order.getOrderItemsList().add(orderItem); // Orderエンティティに注文商品リストを追加
		}

		// 在庫更新処理
		for (OrderItem orderItem : orderItems) {
			Optional<Item> item = itemRepository.findById(orderItem.getItem().getId());
			if (item.isPresent()) {
				Item updatedItem = item.get();
				updatedItem.setStock(updatedItem.getStock() - orderItem.getQuantity());
				itemRepository.save(updatedItem); // 在庫更新
			}
		}

		// 注文情報を保存
		orderRepository.save(order); // 注文をDBに保存

		session.removeAttribute("orderForm"); // セッションから注文情報を削除
		session.removeAttribute("basketBeanList"); // セッションから注文商品リストを削除
		return "redirect:/client/order/complete"; // 注文完了画面へ遷移
	}

	/**
	 * 注文登録完了画面表示の処理
	 *
	 * @return "client/order/complete" 注文完了画面へ
	 */
	@GetMapping("/client/order/complete")
	public String orderCompleteFinish() {
		return "client/order/complete"; // 注文完了画面を表示
	}
}
