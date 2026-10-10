package com.example.memberplatform.security;

import com.example.memberplatform.member.Member;
import com.example.memberplatform.member.MemberRepository;
import com.example.memberplatform.member.MemberService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String rawUsername) throws UsernameNotFoundException {
        String username = MemberService.normalizeUsername(rawUsername);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("會員不存在"));
        return new MemberPrincipal(member);
    }
}
