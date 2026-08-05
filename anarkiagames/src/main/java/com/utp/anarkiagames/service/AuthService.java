    package com.utp.anarkiagames.service;

    import com.utp.anarkiagames.controller.LoginRequest;
    import com.utp.anarkiagames.controller.RegisterRequest;
    import com.utp.anarkiagames.controller.TokenResponse;
    import com.utp.anarkiagames.model.User;
    import com.utp.anarkiagames.repository.UserRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Service;


    @Service
    @RequiredArgsConstructor
    public class AuthService {
        private final PasswordEncoder passwordEncoder;
        private final UserRepository userRepository;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public TokenResponse register(RegisterRequest request){
            var user = User.builder()
                    .nombre(request.name())
                    .email(request.email())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .role("USER")
                    .build();
            var savedUser = userRepository.save(user);
            var jwtToken = jwtService.generateToken(savedUser);
            var refreshToken = jwtService.generateRefreshToken(savedUser);
            return new TokenResponse(jwtToken, refreshToken);
        }

        public TokenResponse login(LoginRequest request){
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
            var user = userRepository.findByEmail(request.email())
                    .orElseThrow();
            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);
            return new TokenResponse(jwtToken, refreshToken);
        }

        public TokenResponse refreshToken(final String authHeader){
            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                throw new IllegalArgumentException("Invalid token");
            }

            final String refreshToken = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(refreshToken);

            if(userEmail == null){
                throw new IllegalArgumentException("Invalid Refresh Token");
            }
            final User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException(userEmail));
            if(!jwtService.isTokenValid(refreshToken, user)){
                throw new IllegalArgumentException("Invalid Refresh Token");
            }

            final String accessToken = jwtService.generateToken(user);
            return new TokenResponse(accessToken, refreshToken);
        }
    }
