package br.com.vitorgsn.todolist.filter;

import java.io.IOException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import at.favre.lib.crypto.bcrypt.BCrypt;
import br.com.vitorgsn.todolist.user.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FilterTaskAuth extends OncePerRequestFilter {

    @Autowired
    private IUserRepository userRepository;

    // Filtro de autenticação
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Coleta os dados de autorização criptografados enviados pelo cabeçalho da
        // requisição http
        var authorization = request.getHeader("Authorization");

        // Remove o "Basic " (o .trim() remove o espaço) da String de autorização
        // criptografada
        var authEncoded = authorization.substring("Basic".length()).trim();

        // Faz o decode da String de autorização criptografada para um array de bytes,
        // remove a criptografia
        byte[] authDecode = Base64.getDecoder().decode(authEncoded);

        // Transforma o array de bytes não legível em uma String legível
        var authString = new String(authDecode);

        // Divide as credenciais username:password que estão numa string única, porém
        // separadas por :
        // Os resultados do split são armazenados em um array de string
        String[] credentials = authString.split(":");

        // Coleta as credenciais do array para strings individuais
        String username = credentials[0];
        String password = credentials[1];

        System.out.println("Authorization");
        System.out.println(username);
        System.out.println(password);

        // Busca de usuário por username, passando o username da requisição
        var user = this.userRepository.findByUsername(username);

        // Se não encontrar um usuário, responde 401 (sem autorização)
        // Se não, valida a senha
        if (user == null) {
            response.sendError(401);
        } else {
            // Utiliza o verify do verifyer da biblioteca BCrypt para comparar o password da
            // requisição com o password (CRIPTOGRAFADA) do usuário encontrado no database
            var passwordVerify = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());

            // Se as senhas forem iguais, autentica o usuário e permite que continue
            // Se não, responde 401 (sem autorização)
            if (passwordVerify.verified) {
                filterChain.doFilter(request, response);
            } else {
                response.sendError(401);
            }
        }
    }

}
