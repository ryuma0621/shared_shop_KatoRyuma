package jp.co.sss.shop.controller.client.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.form.UserForm;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.util.Constant;

/**
 * 会員管理 登録機能(一般会員)のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class ClientUserRegistController {

	/**
	 * 会員情報 リポジトリ
	 */
	@Autowired
	UserRepository userRepository;

	/**
	 * セッション
	 */
	@Autowired
	HttpSession session;

	/**
	 * 入力画面　初期表示処理(GET)
	 * 
	 * @return "client/user/regist_input" 入力画面　表示処理
	 */
	@RequestMapping(path = "/client/user/regist/input/init", method = RequestMethod.GET)
	public String registInput() {
		//ヘッダの新規登録リンクからの遷移

		UserForm userForm = new UserForm() ;
				
		//入力フォーム情報をセッションに保持
		session.setAttribute("userForm", userForm);

		//登録入力画面　表示処理
		return "redirect:/client/user/regist/input";
	}
	
	/**
	 * 入力画面　表示処理(POST) 確認画面での戻るボタン押下後の処理
	 * 
	 * @return "redirect:/client/user/regist/input" 入力画面　表示処理
	 */
	@RequestMapping(path = "/client/user/regist/input", method = RequestMethod.POST)
	public String registInputPost() {

		//セッションスコープより入力情報を取り出す
		UserForm userForm = (UserForm) session.getAttribute("userForm");
		if (userForm == null) {
			// セッション情報がない場合、エラー
			return "redirect:/syserror";
		}

		//登録入力画面　表示処理
		return "redirect:/client/user/regist/input";

	}
	
	/**
	 * 入力画面　表示
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/user/regist_input" 登録入力画面 表示
	 */
	@RequestMapping(path = "/client/user/regist/input", method = RequestMethod.GET)
	public String registInputBack(Model model) {
		
		// セッションから入力フォーム情報取得
		UserForm userForm = (UserForm) session.getAttribute("userForm");
		if (userForm == null) {
			// セッション情報がない場合、エラー
			return "redirect:/syserror";
		}

		BindingResult result = (BindingResult) session.getAttribute("result");
		if (result != null) {
			//セッションにエラー情報がある場合、エラー情報をスコープに設定
			model.addAttribute("org.springframework.validation.BindingResult.userForm", result);
			// セッションにエラー情報を削除
			session.removeAttribute("result");
		}
		
		// 入力フォーム情報をスコープに設定
		model.addAttribute("userForm", userForm);
		
		return "client/user/regist_input";

	}

	/**
	 * 登録入力確認　処理
	 *
	 * @param form 入力フォーム
	 * @param result 入力値チェックの結果
	 * @return 
	 * 	入力値エラーあり："redirect:/client/user/regist/input" 入力録画面　表示処理
	 * 	入力値エラーなし："redirect:/client/user/regist/check" 登録確認画面　表示処理
	 */
	@RequestMapping(path = "/client/user/regist/check", method = RequestMethod.POST)
	public String registInputCheck(@Valid @ModelAttribute UserForm form, BindingResult result) {

		// 入力フォーム情報をセッションに保持
		session.setAttribute("userForm", form);
		
		if (result.hasErrors()) {
			// 入力値にエラーがあった場合、エラー情報をセッションに保持
			session.setAttribute("result", result);
			// 登録入力画面　表示処理
			return "redirect:/client/user/regist/input";
		}

		// 登録確認画面　表示処理 
		return "redirect:/client/user/regist/check";
	}

	/**
	 * 確認画面　表示処理
	 *
	 * @param model Viewとの値受渡し
	 * @return "client/user/regist_check" 確認画面　表示
	 */
	@RequestMapping(path = "/client/user/regist/check", method = RequestMethod.GET)
	public String registCheck(Model model) {
		//セッションから入力フォーム情報取得
		UserForm userForm = (UserForm) session.getAttribute("userForm");
		if (userForm == null) {
			// セッション情報がない場合、エラー
			return "redirect:/syserror";
		}
		//入力フォーム情報をスコープへ設定
		model.addAttribute("userForm", userForm);

		//登録確認画面　表示処理
		return "client/user/regist_check";

	}


	/**
	 * 情報登録処理
	 *
	 * @return "redirect:/client/user/regist/complete" 登録完了画面　表示処理
	 */
	@RequestMapping(path = "/client/user/regist/complete", method = RequestMethod.POST)
	public String registComplete() {

		//セッション保持情報から入力値再取得
		UserForm userForm = (UserForm) session.getAttribute("userForm");
		if (userForm == null) {
			// セッション情報がない場合、エラー
			return "redirect:/syserror";
		}

		// 会員情報を生成
		User user = new User();

		// 入力フォーム情報をエンティティに設定
		BeanUtils.copyProperties(userForm, user);

		// DB登録
		userRepository.save(user);

		//セッションから入力foam情報削除
		session.removeAttribute("userForm");

		// データベースから登録した会員情報を取得(id入りの会員情報)
		user = userRepository.findByEmailAndDeleteFlag(user.getEmail(), Constant.NOT_DELETED);

		UserBean userBean = new UserBean();
		BeanUtils.copyProperties(user, userBean);

		// セッションスコープに会員情報をログインユーザ情報として保持
		session.setAttribute("user", userBean);

		return "redirect:/client/user/regist/complete";
	}
	
	/**
	 * 登録完了画面 表示
	 *
	 * @return "client/user/regist_complete" 登録完了画面 表示
	 */
	@RequestMapping(path = "/client/user/regist/complete", method = RequestMethod.GET)
	public String registCompleteFinish() {
		return "client/user/regist_complete";
	}

}
