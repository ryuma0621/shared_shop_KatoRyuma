package jp.co.sss.shop.controller.client.user;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.util.Constant;

/**
 * 会員管理 表示機能(一般会員)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientUserShowController {
	/**
	 * 会員情報
	 */
	@Autowired
	UserRepository userRepository;

	/**
	 * セッション
	 */
	@Autowired
	HttpSession session;

	/**
	 * 会員情報詳細画面表示処理
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/user/detail" 会員情報詳細表示画面へ
	 */

	@RequestMapping(path = "/client/user/detail", method = { RequestMethod.GET, RequestMethod.POST })
	public String showUser(Model model) {

		// ログインしている会員IDを取得する
		Integer id = ((UserBean) session.getAttribute("user")).getId();

		// 会員情報を取得する
		User user = userRepository.findByIdAndDeleteFlag(id, Constant.NOT_DELETED);

		if (user == null) {
			// 対象が無い場合、エラー
			return "redirect:/syserror" ;
		}

		UserBean userBean = new UserBean();

		// Userエンティティの各フィールドの値をUserBeanにコピー
		BeanUtils.copyProperties(user, userBean);

		// 会員情報をViewに渡す
		model.addAttribute("userBean", userBean);

		//会員登録・変更・削除用のセッションスコープを初期化
		session.removeAttribute("userForm");

		return "client/user/detail";
	}
}
